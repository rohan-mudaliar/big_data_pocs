package com.example.streaming.demo.KafkaStreams.listener.dto;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "order_report", shards = 1, replicas = 0, refreshInterval = "-1")
public class OrderReport {

	@Field(type = FieldType.Text)
	private String orderId;
	@Field(type = FieldType.Text)
	private String itemTypes;
	@Field(type = FieldType.Integer)
	private Integer quantity;
	@Field(type = FieldType.Text)
	private String shipmentPartner;
	@Field(type = FieldType.Text)
	private String shipmentId;
	@Field(type = FieldType.Text)
	private String orderCost;
	@Field(type = FieldType.Text)
	private String shipmentCost;
	@Field(type = FieldType.Text)
	private String itemDescription;
	@Field(type = FieldType.Text)
	private String customerAddress;
	
}
