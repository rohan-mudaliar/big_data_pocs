package com.example.streaming.demo.KafkaStreams.listener;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.streaming.demo.ElasticSearchConsumer.service.ElasticSearchServiceImpl;
import com.example.streaming.demo.KafkaStreams.listener.constants.ElasticSearchTopics;
import com.example.streaming.demo.KafkaStreams.listener.constants.OutBoundConstants;
import com.example.streaming.demo.KafkaStreams.listener.dto.OrderReport;
import com.example.streaming.demo.config.KafkaConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author rohan
 *
 */
@Slf4j
@Component
public class KafkaStreamsIndexCreator {

	@Autowired
	KafkaStreamsListenerOperationsHelperService kafkaStreamsListenerOperationsHelperService;

	@Autowired
	ElasticSearchServiceImpl elasticSearchService;

	@Value("${kafka.logistics.topic}")
	private String logisticsTopic;

	@Value("${kafka.order.topic}")
	private String orderTopic;

	@Value("${kafka.wms.topic}")
	private String wmsTopic;

	@Autowired
	private KafkaConfig kafkaConfig;

	/**
	 * On server start we call construct table method
	 */
	@PostConstruct
	private void start() {
		constructOrUpdateOutBoundReport();
	}

	@PreDestroy
	public void cleanUp() throws Exception {
		System.out.println("Spring Container is destroy! Customer clean up");
	}

	/**
	 * this method is used to create an aggregated index for the Outbound report
	 * Here we are listening to Order.Order_item, WMS.Order_item, WMS.Shipment,
	 * WMS.Manifest to create an aggregated stream
	 */
	public void constructOrUpdateOutBoundReport() {
		log.info("Entering KafkaStreamsListener:constructOutBoundReport");		
		boolean checkIfIndexExists = elasticSearchService
				.checkIfIndexExists(ElasticSearchTopics.OURBOUND_REPORT_TOPIC + "_es");
		// the below is used to listen to streams for the required tables for
		// aggregation
		// we call the stream only if index does not exist

		Properties props = kafkaConfig.populateKafkConfigMap("new-app1");
		final StreamsBuilder stremBuilder = new StreamsBuilder();
		KTable<String, String> orderKTable = stremBuilder.table(orderTopic);
		KTable<String, String> wmsKTable = stremBuilder.table(wmsTopic);
		KTable<String, String> shipmentKtable = stremBuilder.table(logisticsTopic);
		if (!checkIfIndexExists) {
			constructOutBoundReport(orderKTable, wmsKTable, shipmentKtable);
		} 
		final KafkaStreams streams = new KafkaStreams(stremBuilder.build(), props);
		streams.start();
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
		log.info("Exiting KafkaStreamsListener:constructOutBoundReport");
	}


	private void constructOutBoundReport(KTable<String, String> orderKTable, KTable<String, String> wmsKTable,
			KTable<String, String> shipmentKtable) {
		// the input streams needs to be transformed to a consumable format. here we are
		// setting key as shipment_id and value as order item object
		KTable<String, String> orderMap = orderKTable.groupBy((key, value)->{
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value,OutBoundConstants.AFTER);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.ORDER_ID);
			return new KeyValue<>(keyToUse, afterObject.toJSONString());
		}).aggregate(
		        // Initiate the aggregate value
		        () -> null,
		        // adder (doing nothing, just passing the user through as the value)
		        (applicationId, user, aggValue) -> user,
		        // subtractor (doing nothing, just passing the user through as the value)
		        (applicationId, user, aggValue) -> user
		);
		// we are setting the key as order Item Id and value as Order.OrderItem
		KTable<String, String> wmstopicMap = wmsKTable.groupBy((key, value)->{
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value,OutBoundConstants.AFTER);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.ORDER_ID);
			return new KeyValue<>(keyToUse, afterObject.toJSONString());
		}).aggregate(
		        // Initiate the aggregate value
		        () -> null,
		        // adder (doing nothing, just passing the user through as the value)
		        (applicationId, user, aggValue) -> user,
		        // subtractor (doing nothing, just passing the user through as the value)
		        (applicationId, user, aggValue) -> user
		);
		

		// here we are setting key as shipment_id and value as shipment object
		KTable<String, String> shipmentMap = shipmentKtable.groupBy((key, value)->{
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value,OutBoundConstants.AFTER);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.LOGISTICS_ID);
			return new KeyValue<>(keyToUse, afterObject.toJSONString());
		}).aggregate(
		        // Initiate the aggregate value
		        () -> null,
		        // adder (doing nothing, just passing the user through as the value)
		        (applicationId, user, aggValue) -> user,
		        // subtractor (doing nothing, just passing the user through as the value)
		        (applicationId, user, aggValue) -> user
		);
//		

		// we first perform a join on wms.order item and wms.shipment on shipment Id and
		// transform the strem to have ordeItemId as key
		KTable<String, String> mergedStream = orderMap
				.join(wmstopicMap, (leftValue, rightValue) -> kafkaStreamsListenerOperationsHelperService
						.assignValues(leftValue, rightValue))
					.groupBy((key, value)->{
					return createOrderWmsStream(value);
				}).aggregate(
				        // Initiate the aggregate value
				        () -> null,
				        // adder (doing nothing, just passing the user through as the value)
				        (applicationId, user, aggValue) -> user,
				        // subtractor (doing nothing, just passing the user through as the value)
				        (applicationId, user, aggValue) -> user
				);
//		
		// we now join the aggregated table from previous step and Order.Order item
		// table on the Order_item Id
		KTable<String, String> finalStream = mergedStream.join(shipmentMap, (leftValue,
				rightValue) -> kafkaStreamsListenerOperationsHelperService.apendValues(leftValue, rightValue));
		callEsAndCreateIndex(finalStream.toStream());
	}

	private KeyValue<String, String> createOrderWmsStream(String value) {
		OrderReport orderReport = null;
		try {
			orderReport = new ObjectMapper().readValue(value.toString(), OrderReport.class);
		} catch (JsonMappingException e1) {
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		String keyToUse = orderReport.getShipmentId();
		String finalString = kafkaStreamsListenerOperationsHelperService.convertJsontoString(orderReport);
		return new KeyValue<>(keyToUse, finalString);
	}

	/**
	 * this method is takes in the created aggregated stream and created an elastic
	 * search Index
	 * 
	 * @param manifestMap
	 */
	public void callEsAndCreateIndex(KStream<String, String> manifestMap) {
		log.info("Entering KafkaStreamsListener:callEsAndCreateIndex");
		manifestMap.foreach((key, value) -> {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> inputMap = null;
			try {
				inputMap = mapper.readValue(value, Map.class);
				elasticSearchService.pushToEs(ElasticSearchTopics.OURBOUND_REPORT_TOPIC, inputMap);
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		log.info("Exiting KafkaStreamsListener:callEsAndCreateIndex");
	}

}
