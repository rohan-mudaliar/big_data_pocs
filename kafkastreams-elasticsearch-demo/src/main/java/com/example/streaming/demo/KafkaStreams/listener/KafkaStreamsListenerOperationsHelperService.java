package com.example.streaming.demo.KafkaStreams.listener;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import com.example.streaming.demo.KafkaStreams.listener.constants.OutBoundConstants;
import com.example.streaming.demo.KafkaStreams.listener.dto.OrderReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author rohan
 *
 */
@Component
@Slf4j
public class KafkaStreamsListenerOperationsHelperService {

	/**
	 * method to merge the first two data streams from wms i.e OrderItem and
	 * shipment
	 * 
	 * @param leftValue
	 * @param rightValue
	 * @return
	 */
	public String assignValues(String leftValue, String rightValue) {
		log.info("Entering KafkaStreamsListener:assignValues");
		JSONObject orderRecord = null;
		JSONObject warehouseRecord = null;
		orderRecord = extractJson(leftValue);
		warehouseRecord = extractJson(rightValue);
		OrderReport pickListOrderItem = populatePickListOrderItems(orderRecord, warehouseRecord);
		String finalString = convertJsontoString(pickListOrderItem);
		log.info("Entering KafkaStreamsListener:assignValues");
		return finalString;
	}

	/**
	 * method to append the third data stream to the first two
	 * 
	 * @param leftValue
	 * @param rightValue
	 * @return
	 */
	public String apendValues(String leftValue, String rightValue) {
		log.info("Entering KafkaStreamsListener:apendValues");
		OrderReport orderReport = null;
		JSONObject shipmentRecord = null;
		shipmentRecord = extractJson(rightValue);
		try {
			orderReport = new ObjectMapper().readValue(leftValue.toString(), OrderReport.class);
			orderReport.setShipmentCost(setValue(shipmentRecord, OutBoundConstants.SHIPMENT_COST));
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		String finalString = convertJsontoString(orderReport);
		log.info("Entering KafkaStreamsListener:apendValues");
		return finalString;
	}

	/**
	 * utility method
	 * 
	 * @param orderRecord
	 * @param shipment
	 * @return
	 */
	public OrderReport populatePickListOrderItems(JSONObject orderRecord, JSONObject wmsRecord) {
		log.info("Entering KafkaStreamsListener:populatePickListOrderItems");
		OrderReport orderReport = new OrderReport();
		orderReport.setOrderId(setValue(orderRecord, OutBoundConstants.ORDER_ID));
		orderReport.setItemTypes(setValue(wmsRecord, OutBoundConstants.WMS_ITEM_TYPE));
		orderReport.setOrderCost(setValue(orderRecord, OutBoundConstants.ORDER_COST));
		orderReport.setQuantity(Integer.parseInt(setValue(wmsRecord, OutBoundConstants.QTY)));
		orderReport.setShipmentId(setValue(wmsRecord, OutBoundConstants.SHIPMENT_ID));
		orderReport.setShipmentPartner(setValue(wmsRecord, OutBoundConstants.SHIPMENT_PARTNER));
		log.info("Entering KafkaStreamsListener:populatePickListOrderItems");
		return orderReport;
	}




	public String setValue(JSONObject jsonObject, String value) {
		//log.info("Entering KafkaStreamsListener:setValue");
		return jsonObject.get(value) != null ? jsonObject.get(value).toString() : null;
	}

	/**
	 * utility method for json conversion
	 * 
	 * @param pickListOrderItem
	 * @return
	 */
	public String convertJsontoString(OrderReport pickListOrderItem) {
		//log.info("Entering KafkaStreamsListener:convertJsontoString");
		String finalString = null;
		try {
			finalString = new ObjectMapper().writeValueAsString(pickListOrderItem);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		//log.info("Entering KafkaStreamsListener:convertJsontoString");
		return finalString;
	}

	/**
	 * method to extract JSON object from given debezium created kafka topic
	 * 
	 * @param value
	 * @return
	 */
	public JSONObject fetchDto(String value) {
		//log.info("Entering KafkaStreamsListener:fetchDto");
		JSONObject json = extractJson(value);
		JSONObject object = (JSONObject) json.get(OutBoundConstants.PAYLOAD);
		JSONObject afterObject = (JSONObject) object.get(OutBoundConstants.AFTER);
		//log.info("Entering KafkaStreamsListener:fetchDto");
		return afterObject;
	}

	/**
	 * utiity method to extract JSOn from a given value
	 * 
	 * @param value
	 * @return
	 */
	public JSONObject extractJson(String value) {
		//log.info("Entering KafkaStreamsListener:extractJson");
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		try {
			json = (JSONObject) parser.parse(value);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//log.info("Entering KafkaStreamsListener:extractJson");
		return json;
	}

}
