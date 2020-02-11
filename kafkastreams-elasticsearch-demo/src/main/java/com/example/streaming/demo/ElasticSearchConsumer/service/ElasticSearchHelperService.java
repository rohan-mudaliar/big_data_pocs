package com.example.streaming.demo.ElasticSearchConsumer.service;

import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import com.example.streaming.demo.KafkaStreams.listener.constants.ElasticIndexConstants;
import com.example.streaming.demo.KafkaStreams.listener.constants.OutBoundConstants;

@Component
public class ElasticSearchHelperService {

	public Map<String, Object> populateOutBoundDto(Map<String, Object> sourceAsMap, JSONObject extractJson,
			String event) {
		return convertToDto(sourceAsMap,extractJson, event);
	}

	private Map<String, Object> convertToDto(Map<String, Object> sourceAsMap, JSONObject extractJson, String event) {
		if (event.equals(OutBoundConstants.WMS_EVENT)) {
			sourceAsMap.put(ElasticIndexConstants.WMS_QUANITY,extractJson.get(OutBoundConstants.QTY).toString());
			sourceAsMap.put(ElasticIndexConstants.WMS_ITEM_TYPE,extractJson.get(OutBoundConstants.WMS_ITEM_TYPE).toString());
		}
		else if (event.equals(OutBoundConstants.ORDER_EVENT)) {
			sourceAsMap.put(ElasticIndexConstants.ORDER_ORDER_COST,extractJson.get(OutBoundConstants.ORDER_COST).toString());
			sourceAsMap.put(ElasticIndexConstants.ORDER_ITEM_DESCRIPTION,extractJson.get(OutBoundConstants.ORDER_DESCRIPTION).toString());
			sourceAsMap.put(ElasticIndexConstants.ORDER_CUSTOMER_ADDRESS,extractJson.get(OutBoundConstants.CUSTOMER_ADDRESS).toString());
		}
		else if (event.equals(OutBoundConstants.LOGISTICS_EVENT)) {
			sourceAsMap.put(ElasticIndexConstants.LOGISTICS_SHIPMENTPARTNER,extractJson.get(OutBoundConstants.COURIER).toString());
			sourceAsMap.put(ElasticIndexConstants.LOGISTICS_SHIPMENTCOST,extractJson.get(OutBoundConstants.SHIPMENT_COST).toString());
		}
		return sourceAsMap;
	}

}
