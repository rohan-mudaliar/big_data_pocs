package com.example.streaming.demo.KafkaStreams.listener;

import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.streaming.demo.ElasticSearchConsumer.service.ElasticSearchServiceImpl;
import com.example.streaming.demo.KafkaStreams.listener.constants.OutBoundConstants;
import com.example.streaming.demo.config.KafkaConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author rohan
 *
 */
//@Component
@Slf4j
public class KakfaStreamsTopicListener {

	/**
	 * the below are the list of topics which we listen to and update individual indexes
	 */
	@Value("${kafka.logistics.topic}")
	private String logisticsTopic;

	@Value("${kafka.order.topic}")
	private String orderTopic;

	@Value("${kafka.wms.topic}")
	private String wmsTopic;

	@Autowired
	private KafkaConfig kafkaConfig;

	@Autowired
	ElasticSearchServiceImpl elasticSearchServiceImpl;
	@Autowired
	KafkaStreamsListenerOperationsHelperService kafkaStreamsListenerOperationsHelperService;

	/**
	 * On server start we call construct table method
	 */
	@PostConstruct
	private void start() {
		wmsUpdates();
		orderServiceUpdates();
		logisticsServiceUpdates();
	}

	/***
	 * The below method is used to listen to wms_order_item events and update the corresponding indexes
	 */
	public void wmsUpdates() {
		log.info("Entering KafkaStreamsListener:constructOutBoundReport");
		Properties props = kafkaConfig.populateKafkConfigMap();
		final StreamsBuilder stremBuilder = new StreamsBuilder();
		KStream<String, String> orderItemStream = stremBuilder.stream(wmsTopic);
		//we listen to the incoming stream and transform it to a new stream with the key as order_item Id and value as json input
		KStream<String, String> orderMap = orderItemStream.map((key, value) -> {
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.ORDER_ID);
			return new KeyValue<>(keyToUse, afterObject.toJSONString());
		});
		//for each record we update the corresponding indices in elastic search
		orderMap.foreach((key, value) -> {
			elasticSearchServiceImpl.updateIndexForWmsEvent(key, value);
		});
		final KafkaStreams streams = new KafkaStreams(stremBuilder.build(), props);
		streams.start();
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}

	/***
	 * The below method is used to listen to order_order_item events and update the corresponding indexes
	 */
	public void orderServiceUpdates() {
		log.info("Entering KafkaStreamsListener:constructOutBoundReport");
		Properties props = kafkaConfig.populateKafkConfigMap();
		final StreamsBuilder stremBuilder = new StreamsBuilder();
		KStream<String, String> orderItemStream = stremBuilder.stream(logisticsTopic);
		//we listen to the incoming stream and transform it to a new stream with the key as order_item Id and value as json input
		KStream<String, String> orderMap = orderItemStream.map((key, value) -> {
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.WMS_ITEM_TYPE);
			return new KeyValue<>(keyToUse, afterObject.toJSONString());
		});
		//for each record we update the corresponding indices in elastic search
		orderMap.foreach((key, value) -> {
			elasticSearchServiceImpl.updateIndexForOrderEvent(key, value);
		});
		final KafkaStreams streams = new KafkaStreams(stremBuilder.build(), props);
		streams.start();
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}

	public void logisticsServiceUpdates() {
		log.info("Entering KafkaStreamsListener:constructOutBoundReport");
		Properties props = kafkaConfig.populateKafkConfigMap();
		final StreamsBuilder stremBuilder = new StreamsBuilder();
		KStream<String, String> orderItemStream = stremBuilder.stream(logisticsTopic);
		//we listen to the incoming stream and transform it to a new stream with the key as order_item Id and value as json input
		KStream<String, String> orderMap = orderItemStream.map((key, value) -> {
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.WMS_ITEM_TYPE);
			return new KeyValue<>(keyToUse, afterObject.toJSONString());
		});
		//for each record we update the corresponding indices in elastic search
		orderMap.foreach((key, value) -> {
			elasticSearchServiceImpl.updateIndexForOrderEvent(key, value);
		});
		final KafkaStreams streams = new KafkaStreams(stremBuilder.build(), props);
		streams.start();
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	}

}
