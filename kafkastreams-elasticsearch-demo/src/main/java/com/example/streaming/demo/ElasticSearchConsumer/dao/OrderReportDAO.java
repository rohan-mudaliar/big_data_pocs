package com.example.streaming.demo.ElasticSearchConsumer.dao;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.springframework.stereotype.Repository;

/***
 * 
 * @author rohan
 *
 */
@Repository
public class OrderReportDAO {
	private RestHighLevelClient restHighLevelClient;

	public OrderReportDAO(RestHighLevelClient restHighLevelClient) {
		this.restHighLevelClient = restHighLevelClient;
	}

	/***
	 * This method is used to create a new index in elastic search
	 * 
	 * @param tableName
	 * @param payloadMap
	 * @return
	 */
	public RestStatus insertIndex(String tableName, Map<String, Object> payloadMap,String id) {
		IndexResponse indexResponse = null;
		IndexRequest indexRequest = new IndexRequest(tableName + "_es")
				// .id(payloadMap.get("id").toString())
				.type("_doc").id(id).source(payloadMap);
		try {
			indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return indexResponse.status();
	}

	public Map<String, Object> getIndexById(String id,String table) {
		GetRequest getRequest = new GetRequest(table, "_doc", id);
		 Map<String, Object> sourceAsMap = null;
		try {
			GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
			String index = getResponse.getIndex();
			String docId = getResponse.getId();
			if (getResponse.isExists()) {
			    long version = getResponse.getVersion();
			    String sourceAsString = getResponse.getSourceAsString();        
			    sourceAsMap = getResponse.getSourceAsMap(); 
			    byte[] sourceAsBytes = getResponse.getSourceAsBytes();          
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sourceAsMap;
	}

	/**
	 * API to update a document in WMSOUtBound report
	 * @param ourboundReportTopic
	 * @param sourceAsMap
	 * @param orderItemId 
	 */
	public void updateIndex(String outboundReportTopic, Map<String, Object> sourceAsMap, String orderItemId) {
		UpdateRequest request = new UpdateRequest().index(outboundReportTopic).id(orderItemId).type("_doc")
		        .doc(sourceAsMap);
		try {
			UpdateResponse updateResponse = restHighLevelClient.update(
			        request, RequestOptions.DEFAULT);
			System.out.println(updateResponse.getGetResult());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
