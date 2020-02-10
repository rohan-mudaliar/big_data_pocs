package com.example.streaming.demo.ElasticSearchConsumer.service;

import java.io.IOException;
import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.streaming.demo.ElasticSearchConsumer.dao.OrderReportDAO;
import com.example.streaming.demo.KafkaStreams.listener.KafkaStreamsListenerOperationsHelperService;
import com.example.streaming.demo.KafkaStreams.listener.constants.ElasticSearchTopics;
import com.example.streaming.demo.KafkaStreams.listener.constants.OutBoundConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by raman on 06/01/20
 */
@Service
@Slf4j
public class ElasticSearchServiceImpl {

	@Autowired
	ElasticSearchHelperService elasticSearchHelperService;

	@Autowired
	private OrderReportDAO wmsOutBoundReportDAO;
	@Autowired
	KafkaStreamsListenerOperationsHelperService kafkaStreamsListenerOperationsHelperService;

	/**
	 * method to create an index in elastic search
	 * @param tableName
	 * @param payloadMap
	 * @throws IOException
	 */
	public void pushToEs(String tableName, Map<String, Object> payloadMap) throws IOException {
		log.info("Pushing to table {} with payload: {}", tableName, payloadMap);
		// this should only be used for creates
		String id = (String) payloadMap.get("orderId");
		boolean checkIfIndexExists = wmsOutBoundReportDAO.checkIfIndexExists(ElasticSearchTopics.OURBOUND_REPORT_TOPIC+ "_es");
		if(!checkIfIndexExists) {
				wmsOutBoundReportDAO.insertIndex(tableName, payloadMap,id);
		}
		
	}

	/**
	 * method to update the WmsOutBoundReport for wms_order_item event
	 * @param orderItemId
	 * @param jsonValue
	 */
	public void updateIndexForWmsEvent(String orderItemId, String jsonValue) {
		JSONObject extractJson = kafkaStreamsListenerOperationsHelperService.extractJson(jsonValue);
		// we get the documents that have the same order item Id
		Map<String, Object> sourceAsMap = wmsOutBoundReportDAO.getIndexById(orderItemId, ElasticSearchTopics.OURBOUND_REPORT_TOPIC + "_es");
		// update the original index with new values from the input stream
		// push the updated document to elastic search
		if(sourceAsMap!=null) {
			sourceAsMap = elasticSearchHelperService.populateOutBoundDto(sourceAsMap, extractJson, OutBoundConstants.WMS_ORDER_ITEM_EVENT);
		updateIndex(ElasticSearchTopics.OURBOUND_REPORT_TOPIC+ "_es", sourceAsMap,orderItemId);
		}
	}

	/**
	 * method to update the WmsOutBoundReport for order_order_item_event
	 * @param orderItemId
	 * @param jsonValue
	 */
	public void updateIndexForOrderEvent(String orderItemId, String jsonValue) {
		JSONObject extractJson = kafkaStreamsListenerOperationsHelperService.extractJson(jsonValue);
		Map<String, Object> sourceAsMap = wmsOutBoundReportDAO.getIndexById(orderItemId, ElasticSearchTopics.OURBOUND_REPORT_TOPIC + "_es");
		// update the original index with new values from the input stream
		sourceAsMap = elasticSearchHelperService.populateOutBoundDto(sourceAsMap, extractJson,
				OutBoundConstants.WAREHOUSE_ID);
		// push the updated document to elastic search
		updateIndex(ElasticSearchTopics.OURBOUND_REPORT_TOPIC, sourceAsMap,orderItemId);
	}

	/**
	 * method to update a document in ES
	 * @param ourboundReportTopic
	 * @param sourceAsMap
	 * @param orderItemId 
	 */
	public void updateIndex(String ourboundReportTopic, Map<String, Object> sourceAsMap, String orderItemId) {
		wmsOutBoundReportDAO.updateIndex(ElasticSearchTopics.OURBOUND_REPORT_TOPIC, sourceAsMap,orderItemId);

	}

}