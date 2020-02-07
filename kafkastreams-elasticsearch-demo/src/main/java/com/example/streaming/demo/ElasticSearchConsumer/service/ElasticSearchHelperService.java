package com.example.streaming.demo.ElasticSearchConsumer.service;

import java.util.Map;

import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;

import com.example.streaming.demo.KafkaStreams.listener.constants.OutBoundConstants;

@Component
public class ElasticSearchHelperService {

	public Map<String, Object> populateOutBoundDto(Map<String, Object> sourceAsMap, JSONObject extractJson,
			String event) {
		return convertToDto(sourceAsMap,extractJson, event);
	}

	private Map<String, Object> convertToDto(Map<String, Object> sourceAsMap, JSONObject extractJson, String event) {
		if (event.equals(OutBoundConstants.WMS_ORDER_ITEM_EVENT)) {
			sourceAsMap.put("awbNumber",extractJson.get(OutBoundConstants.SHIPMENT_ID).toString());
		}
		else if(event.equals(OutBoundConstants.WAREHOUSE_ID)) {
			//sourceAsMap.put("orderCreatedTimeStamp",extractJson.get(OutBoundConstants.ORDER_ORDER_ITEM_CREATED_TIMESTAMP));
			sourceAsMap.put("transactionType", extractJson.get(OutBoundConstants.ORDER_ORDER_ITEM_TRANSACTION_TYPE));
		}
		return sourceAsMap;
	}

}
