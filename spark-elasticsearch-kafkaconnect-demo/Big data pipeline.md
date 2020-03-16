Building a Real time Streaming system with Apache Kafka connect,Debezium, Kafka Streams,Elastic Search and Mysql

# Introduction



With the exponential growth of data and lot of business moving online, it has become imperative to design systems that can act real time or near real time to take any business decisions. So after working on multiple backend projects through many years I finally got to do build a  streaming platform. And while working on the project I did start experiementing on the different tech stacks to deal with this. So I am trying to share my learnings in a series of articles. Here is the first of them.

Before we dive into the problem statement one thing that I did want to put forth was that in this poc I am building a Complex Event Processing (CEP) system. There are very subtle differences between stream processing ,real time processing and complex event processing. They are as below:-

- **Stream Processing:** Stream processing is useful for tasks like fraud detection and cybersecurity. If transaction data is stream-processed, fraudulent transactions can be identified and stopped before they are even complete.
- **Real-time Processing:** If event time is very relevant and latencies in the second's range are completely unacceptable then it’s called Real-time (Rear real-time) processing. For ex. flight control system for space programs
- **Complex Event Processing (CEP):** CEP utilizes event-by-event processing and aggregation (for example, on potentially out-of-order events from a variety of sources, often with large numbers of rules or business logic).

This depends more on the business use case. The same tech stack can be used for stream processing as well as real time processing. 

### Problem Statement

Imagine that you work for an E-Commerce company selling some fashion products. You have the business team that wants to make some decisions based on some real-time updates. They want to view some dashboards and views which is derived in real time. So lets assume that the backend is build on microservice architecture and you have multipe systems interacting with each other during any user  operation. And each sub system is using  a different database. 

![](/Users/apple/Desktop/TUIN_PIPELINE.jpeg)



Now we see that we have different systems and different databases used by each system. Lets consider three of the systems for our POC.



![Blank Diagram (1)](/Users/apple/Downloads/Blank Diagram (1).jpeg)                                                  Lets say the business team wants to create an aggregated real time dashboard that contains information from the above systems . So for this example they want to understand the Inventory dashboard to better price their pricing strategies.i.e understand number of orders, types of items sold, cost of each item and cost of shipping the items. This information is not present in a single system . This information would be present in multiple systems which interact with multiple databases and this would be constantly updated . That is the order status would change when items are picked from warehouse, picked from shipping etc. Consider that the busines team wants to build a dashboard using this data and they would use this to make some marketting decisions. Lets have a look at the data required by business team:-

- Units shipper per day per item type per warehouse.
- Orders shipped per day with each courier company.
- Total cost of items shipped per day per warehouse.



**So the first step to get to the above data is to first create an aggregated view of information**. All this data is present in  3 different databases used in 3 different systems from where we would need to get our information. Order Service, Warehouse service and logistics service. Assume all of them are using mysql database Each system is updated in real-time when customers place an order on the e-commerce site. So now that we have looked at the use case, lets think about what could our solution to this problem.



**Possible Solution**: We need to figure out a way to capture all the updates/inserts that happen in the different databases in different services and put it in a single place from where we can work on building some reports and working on some analytics. So this is where **kafka connect and debezium** comes in.

### Understanding CDC, Mysql Binlogs and Debezium:

So before we jump into the implementation of this system we will need to understand a few concepts. They are as below:-

**Change data capture:**

Lets consider our application that we have, we have order and warehouse services which interact with mysql databases. Every user operation would be captured in the database. The  concept of capturing all the database changes from multiple databases to a single source is what CDC essentially means. We can enable CDC for multiple databases and stream the changes to a single source. Debezium is an example of once such open source tool that is used for change data capture which streams data from multiple source databases to one single sink i.e Kakfa

**Mysql Binlogs**

The binary log is a set of log files that contain information about data modifications made to a MySQL server instance. The log is enabled by starting the server with the `--log-bin` option.

The binary log was introduced in MySQL 3.23.14. It contains all statements that update data. It also contains statements that potentially could have updated it (for example, a `DELETE` which matched no rows), unless row-based logging is used. Statements are stored in the form of "events" that describe the modifications. 

**OpLogs:**

Similar to mysql binlogs mongoDb has something called Oplogs which are similar to bin logs and is used for CDC.

### **Tech Stack:**

So now that we do have an overall picture in terms of what we want to achieve and what are the concepts that are involved, next step is to understand the overall technical tasks and the tech stack that we would be using to build our system. Lets take a look at the same.



let's dive into the tech stack that we would be using for this POC. They are as below:-



- Mysql 8.0.18
- Apache Kafka connect 
- Apache Kafka 
- Apache Kafka Streams
- Elastic search.
- Docker
- Swagger UI
- Postman

- We have understood the problem statement that we are dealing with , a possible solution and the different concepts.
- Setup mysql database for binlog events.
- Used debezium kafka connect to create topics in local kafka.

### Overall Technical Tasks:

So in terms of overall tasks, we will be splitting them up as below :-

1. Setting up local infrastructure using docker.
2. Data Ingestion into kafka from mysql database using kafka connect.
3. Reading data using kafka streams in Java backend.
4. Creating indices on elasticsearch for the aggregated views.
5. Listening to the events in real time and updating the same.

### Setting up local infrastructure using docker

So for the readers who do not know what docker is,"Docker is an open source platform for developing, shipping, and running applications. Docker enables you to separate your applications from your infrastructure so you can deliver software quickly."  For more information please do check 

So I am using docker to create the below images in my local:-

- Elasticsearch 7.5.1
- Kibana:7.5.1
- zookeeper:1.0
- kafka:1.0
- kafka Connect 1.0

I am attaching the docker file used for reference below:

```dockerfile
version: "3.5"
services:
  # Install ElasticSearch
  elasticsearch:
    container_name: elasticsearch
    image: docker.elastic.co/elasticsearch/elasticsearch:7.5.1
    environment:
      - discovery.type=single-node
    ports:
      - 9200:9200
      - 9300:9300
  kibana:
    container_name: kibana
    image: docker.elastic.co/kibana/kibana:7.5.1
    environment:
      - elasticsearch.url= http://localhost:9200
    ports:
      - 5601:5601

#debezium
  zookeeper:
      image: debezium/zookeeper:1.0
      hostname: zookeeper
      container_name: zookeeper
      ports:
        - 2181:2181
        - 2888:2888
      environment:
        ZOOKEEPER_CLIENT_PORT: 2181
        ZOOKEEPER_TICK_TIME: 2000
  kafka:
      image: debezium/kafka:1.0
      ports:
        - 9092:9092
      links:
        - zookeeper
      environment:
        - ZOOKEEPER_CONNECT=zookeeper:2181
        - ADVERTISED_HOST_NAME=192.168.0.250
        # If we wanted to connect to Kafka from outside of a Docker container, then we’d want Kafka to advertise its address via the Docker host,
        # which we could do by adding -e ADVERTISED_HOST_NAME= followed by the IP address or resolvable hostname of the Docker host,
        # which on Linux or Docker on Mac this is the IP address of the host computer (not localhost).


  schema-registry:
      image: confluentinc/cp-schema-registry
      ports:
        - 8181:8181
        - 8081:8081
      environment:
        - SCHEMA_REGISTRY_KAFKASTORE_CONNECTION_URL=zookeeper:2181
        - SCHEMA_REGISTRY_HOST_NAME=schema-registry
        - SCHEMA_REGISTRY_LISTENERS=http://schema-registry:8081
      links:
        - zookeeper
  connect:
    image: debezium/connect:1.0
    ports:
      - 8083:8083
    links:
      - kafka
      - schema-registry
    environment:
      - BOOTSTRAP_SERVERS=kafka:9092
      - GROUP_ID=1
      - CONFIG_STORAGE_TOPIC=my_connect_configs
      - OFFSET_STORAGE_TOPIC=my_connect_offsets
      - STATUS_STORAGE_TOPIC=my_connect_statuses
      - INTERNAL_KEY_CONVERTER=org.apache.kafka.connect.json.JsonConverter
      - INTERNAL_VALUE_CONVERTER=org.apache.kafka.connect.json.JsonConverter
```



Assuming you have your docker installed in your local, you need to run the below command  in terminal.

```bash
docker-compose up 
```

This will create the images and bring up the above mentioned services. To verify the same we can can manually check if the services are up:-

 Kafka connect-`curl -H "Accept:application/json" localhost:8083/`

Kibana should be accessible via the below url.

```html
http://localhost:5601/app/kibana#/management/kibana/index_pattern
```

You can manually check for the other services by checking if there are active processes runnin the respective ports (i.e Kafka-9092, zookeeper-2181,elastic search-9200)



### Data Ingestion into kafka from mysql database using kafka connect.

In step 1 we have created the infrastructure required and we have kafka connect up. In this step we need to do the following sub tasks:-

- #### Preparing Mysql for real time updates and give appropriate permissions to user to access database

So for real time updates it is not enough if we just get the data from the different databases, but we also need to get a snapshot of the different updates, deletes etc performed on different systems.

So Mysql has something called Binlogs which helps with this and MongoDb has something called oplog(<https://docs.mongodb.com/manual/core/replica-set-oplog/>).

Setting up of mysql is a DB task and I am attaching a reference of an article I found that would help you with the same:

<https://www.anicehumble.com/2016/12/enabling-mysql-binary-logging-on-macos.html>

A MySQL user must be defined that has all of the following permissions on all of the databases that the connector will monitor:

- [`SELECT`](http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html#priv_select) - enables the connector to select rows from tables in databases; used only when performing a snapshot
- [`RELOAD`](http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html#priv_reload) - enables the connector of the [`FLUSH`](http://dev.mysql.com/doc/refman/5.7/en/flush.html) statement to clear or reload various internal caches, flush tables, or acquire locks; used only when performing a snapshot
- [`SHOW DATABASES`](http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html#priv_show-databases) - enables the connector to see database names by issuing the `SHOW DATABASE` statement; used only when performing a snapshot
- [`REPLICATION SLAVE`](http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html#priv_replication-slave) - enables the connector to connect to and read the binlog of its MySQL server; always required for the connector
- [`REPLICATION CLIENT`](http://dev.mysql.com/doc/refman/5.7/en/privileges-provided.html#priv_replication-client) - enables the use of `SHOW MASTER STATUS`, `SHOW SLAVE STATUS`, and `SHOW BINARY LOGS`; always required for the connector

Read up more on the configurations for mysql at the offical debezium mysql documentation(<https://debezium.io/documentation/reference/1.0/connectors/mysql.html>)

- #### Create a mysql connector using kafka connect.

Once you do have binlogs enabled on your mysql server, next step is to create a connector. To explain in simple terms connector creates a connection from the source system and sends the data read to kafka sink or reads from a kafka source and pushes to a sink system.

We are using the debezium connector(<https://www.confluent.io/hub/debezium/debezium-connector-mysql>) for our purpose. This would read all the DDL's and DML's from binlogs of mysql and push into kafka creating kafka topics. 

Simplest way to do this is via postman . Hit localhost:8083/connectors/ with the below sample request.

```json
{ 
"name":"test-order-connector",
"config":{ 
"connector.class":"io.debezium.connector.mysql.MySqlConnector",
"tasks.max":"1",
"database.hostname":"localhost",
"database.port":"3306",
"database.user":"root",
"database.password":"password",
"database.server.id":"1",
"database.server.name":"test",
"database.whitelist":"wms",
"database.history.kafka.bootstrap.servers":"kafka:9092",
"database.history.kafka.topic":"test.wms"
}
}
```

In the above json most of the keywords are self explainatory, one thing to note is you always need to change database.server.id, database.server.name  and name everytime you create a new connector topic. 

So after running the above json on postman/curl,run the below command to list the kafka topics in your local:-

```bash
kafka-topics --zookeeper localhost:2181 --list
```

The newly created topics would be of the type.

**<database.server.name>. < databasename >.< tablename >**

For our example our table name would be something like

**test.wms.order** 

when you do run the above command you should see tables of the above format.

**Trouble shooting tips:**

There are times when the topic is not created in the given format, this would mean the data has not flown from mysql to kafka topic. So there could be the below reasons for the this:-

- Mysql bin logs has been not configured properly . Check the binlogs in mysql manually to verify if bin logs are written.
- Database user does not have required permissions to access bin logs. This is another common problem that I did run into. Check debezium documentation(<https://debezium.io/documentation/reference/1.0/connectors/mysql.html>) for particular permissions required.
- Database server id has to be unique every time.

So if you do see the topic name, next step is to verify that the topics do have content. Use the below command for the same:-

```bash
 kafka-console-consumer --bootstrap-server 192.168.0.118:9092 --topic test.wms.order --from-beginning
```

192.168.0.118 is the ip of my system. Replace the same with your local ip.

So once this is verified we have now successfully enabled data flow from mysql to kafka topics, next step is to read the kafka topics and create the aggregated views.



### Reading Data using kafka streams in Java backend for First time- Index creation.

So now that we have our kafka topics created, next step is read the streaming data. There are multiple platforms that can be used. In this article we are going to explore the streaming capabilities of kafka streams. Let me break this down into steps:-

1. Decide on the topics you want to listen to which will be used to create the aggregated views.
2. Decide on the tables(topics) you would have to join or transformations you would want to perform on the tables(topics) to arrive at your final aggregated tables.
3. Once you have your final aggregated view contructed, call elastic search to create an Index.

Lets look at point 1 and 2 via code.

```java

@Value("${kafka.logistics.topic}")
	private String logisticsTopic;

	@Value("${kafka.order.topic}")
	private String orderTopic;

	@Value("${kafka.wms.topic}")
	private String wmsTopic;

public void constructOutBoundReport() {
		log.info("Entering KafkaStreamsListener:constructOutBoundReport");
		Properties props = kafkaConfig.populateKafkConfigMap();
		final StreamsBuilder stremBuilder = new StreamsBuilder();
		//the below is used to listen to streams for the required tables for aggregation
		KStream<String, String> logisticsStream = stremBuilder.stream(logisticsTopic);
		KStream<String, String> orderStream = stremBuilder.stream(orderTopic);
		KStream<String, String> wmsStream = stremBuilder.stream(wmsTopic);    
        stremBuilder.stream(orderServiceOrderItemTopic);
    	final KafkaStreams streams = new KafkaStreams(stremBuilder.build(), props);
		streams.start();
		Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
		log.info("Exiting KafkaStreamsListener:constructOutBoundReport");
}
```

Here we are listening to logisticstopic,wmstopic and order topic. We read them and convert them into Kstream first.

Before we arrive at our final aggregated view, we need to create few intermediate aggregations. We are processing the streams and setting order_id(common attribute between order table and wms table) as key.

For the shipment stream we are setting shipmentId as the key.



```java
KStream<String, String> orderItemStream = stremBuilder.stream(orderTopic);                                                                     
KStream<String, String> wmsStream = stremBuilder.stream(wmsTopic);                                                                             
KStream<String, String> shipmentStream = stremBuilder.stream(logisticsTopic);                                                                  
                                                                                                                                               
                                                                                                                                               
//the input streams needs to be transformed to a consumable format. here we are setting key as shipment_id and value as order item object      
KStream<String, String> orderMap = orderItemStream.map((key, value) -> {                                                                       
	JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);                                                      
	String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,                                                        
			OutBoundConstants.ORDER_ID);                                                                                                       
	return new KeyValue<>(keyToUse, afterObject.toJSONString());                                                                               
});                                                                                                                                            
                                                                                                                                               
//we are setting the key as order Item Id and value as Order.OrderItem                                                                         
		KStream<String, String> wmstopicMap = wmsStream.map((key, value) -> {                                                                  
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);                                              
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,                                                
					OutBoundConstants.ORDER_ID);                                                                                               
			return new KeyValue<>(keyToUse, afterObject.toJSONString());                                                                       
		});                                                                                                                                    
		                                                                                                                                       
//here we are setting key as shipment_id and value as shipment object                                                                          
KStream<String, String> shipmentMap = shipmentStream.map((key, value) -> {                                                                     
	JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);                                                      
	String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,                                                        
			OutBoundConstants.LOGISTICS_ID);                                                                                                   
	return new KeyValue<>(keyToUse, afterObject.toJSONString());                                                                               
});                                                                                                                                            
```

Next we would have to create joins on the streams for us to create our aggregated view. We **join order and wms table on orderId** to create an aggregated view with shipmentId as key. We join this view with the shipment stream to create the final aggragate view required.

```java
                                                                                                                                       
// we first perform a join on wms.order item and wms.shipment on shipment Id and transform the strem to have ordeItemId as key         
KStream<String, String> mergedStream = orderMap                                                                                        
		.join(wmstopicMap, (leftValue, rightValue) -> kafkaStreamsListenerOperationsHelperService                                      
				.assignValues(leftValue, rightValue), JoinWindows.of(TimeUnit.MINUTES.toMillis(5)))                                    
		.map((key, value) -> {                                                                                                         
			OrderReport orderReport = null;                                                                                            
			try {                                                                                                                      
				orderReport= new ObjectMapper().readValue(value.toString(), OrderReport.class);                                        
			} catch (JsonMappingException e1) {                                                                                        
				e1.printStackTrace();                                                                                                  
			} catch (JsonProcessingException e1) {                                                                                     
				e1.printStackTrace();                                                                                                  
			}                                                                                                                          
			String keyToUse = orderReport.getShipmentId();                                                                             
			String finalString = kafkaStreamsListenerOperationsHelperService                                                           
					.convertJsontoString(orderReport);                                                                                 
			return new KeyValue<>(keyToUse, finalString);                                                                              
		});                                                                                                                            
                                                                                                                                       
//we now join the aggregated table from previous step and Order.Order item table on the Order_item Id                                  
KStream<String, String> finalStream = mergedStream.join(shipmentMap, (leftValue,                                                       
		rightValue) -> kafkaStreamsListenerOperationsHelperService.apendValues(leftValue, rightValue),                                 
		JoinWindows.of(TimeUnit.MINUTES.toMillis(5)));                                                                                 
```

**assignValues** and **apendValue** methods are used purely used for setting values to the aggregated views.



### Creating indices on elasticsearch for the aggregated views.

Lets look at the controller class method

```java
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


```

We create a map in the controller and we call the service class which would interact with the DAO for elastic search operations. Lets look at the DAO class which is used to perist data into elastic search.

```java
public RestStatus insertIndex(String tableName, Map<String, Object> payloadMap,String id) {
		IndexResponse indexResponse = null;
		IndexRequest indexRequest = new IndexRequest(tableName + "_es")
				 .id(payloadMap.get("id").toString())
				.type("_doc").id(id).source(payloadMap);
		try {
			indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return indexResponse.status();
	}
```

So Elastic search has a java high level rest API which can be used for operations to work with elastic search. Here we are using IndexRequest to create a new index in elastic search. 

```java
IndexRequest indexRequest = new IndexRequest(tableName + "_es")
				 .id(payloadMap.get("id").toString())
				.type("_doc").id(id).source(payloadMap);
```

The above statement is used to create a new request to create an elasticsearch Index. 

We create an instance of **RestHighLevelClient**  as mentioned earlier to interact with elastic search from java.

```java
restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
```

The above statement basically does the creation of indices in elastic search.

### Enabling real time event based updates to system

Now that we have written code to create aggregate views required for business, next step is to make updates to the created index based on individual event updates. Lets have a look at how we can go about this. Let us look at how we do this for one of the events from wms system. We can replicate the same for other systems as well.



```java
public void wmsUpdates() {
		log.info("Entering KafkaStreamsListener:constructOutBoundReport");
		Properties props = kafkaConfig.populateKafkConfigMap();
		final StreamsBuilder stremBuilder = new StreamsBuilder();
		KStream<String, String> orderItemStream = stremBuilder.stream(wmsTopic);
		//we listen to the incoming stream and transform it to a new stream with the key as order_item Id and value as json input
		KStream<String, String> orderMap = orderItemStream.map((key, value) -> {
			JSONObject afterObject = kafkaStreamsListenerOperationsHelperService.fetchDto(value);
			String keyToUse = kafkaStreamsListenerOperationsHelperService.setValue(afterObject,
					OutBoundConstants.WMS_ITEM_TYPE);
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
```

what we are doing in the above piece of code is 3 things, listen to the wms event, once we have that event, we update the existing document with the new information from the event and update the particular document in elasticsearch.

The above is the controller for working with updates. Lets look at the DAO for the same.

```java
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
```

So what I am doing here is I am using UpdateRequest which is the java high level client class provided to work with updates to elasticsearch indices. We again use **RestHighLevelClient** to update the index.

### Viewing the created indices,verifying  updates in Kibana and extracting the reports:



Now that we have created our indices, next step is to verify that the indices are created through kibana and extract a pdf to be given to the business users. Lets see how we can go about the same.

Go to Kiabana from browser:

<<http://localhost:5601/app/kibana#/dev_tools/console>?_g=()

Click on dev tools(one with the screwdriver symbol) and you can execute your command here.  Below are the command used to verify if the index is created

```
GET _cat/indices
GET latestoutboundreport_es/_search
{
    "query": {
        "match_all": {}
    }
}
```





### Viewing the created indices and Verifying  updates in Kibana 

**Viewing the created Index:**

Now that we have created our indices, next step is to verify that the indices are created through kibana and extract a pdf to be given to the business users. Lets see how we can go about the same.

Go to Kiabana from browser:

<http://localhost:5601/app/kibana#/dev_tools/console?_g=()

Click on dev tools(one with the screwdriver symbol) and you can execute your command here.  Below are the command used to verify if the index is created

```sql
GET _cat/indices
```

![Screenshot 2020-01-28 at 2.07.16 AM](/Users/apple/Desktop/Screenshot 2020-01-28 at 2.07.16 AM.png)

In my example latestoutboundreport_es is the index created.



Next we need to verify there is data inside the index,run the below command:-

```sql
GET latestoutboundreport_es/_search
{
    "query": {
        "match_all": {}
    }
}
```

here latestoutboundreport_es is my index created. On executing the above, we can see records  on the right panel.



![Screenshot 2020-02-11 at 4.16.42 PM](/Users/apple/Desktop/Screenshot 2020-02-11 at 4.16.42 PM.png)

### Testing Updates



To test the event streaming and real time updates, we will need to simulate this scenario. We can test this by running an update to the three tables that were used to create the aggregate index.



The sql queries used are as below:



Before Update:

![Screenshot 2020-02-11 at 4.16.42 PM](/Users/apple/Desktop/Screenshot 2020-02-11 at 4.16.42 PM.png)

After Update

![Screenshot 2020-02-11 at 4.20.06 PM](/Users/apple/Desktop/Screenshot 2020-02-11 at 4.20.06 PM.png)

I am updating the quantity(1 to 2) and item description(64 GB to 128 GB)



**Exporting the CSV:**

So now that we have the records next is to export this data out into a pdf, luckily Kibana does have this option of exporting CSV.

Click on discover tab, this shows the continous stream of data being received. Now you can provide your filters for searching and once your search result appears.



 click on save and export csv. Refer the screenshots below;-

![Screenshot 2020-01-28 at 2.13.29 AM](/Users/apple/Desktop/Screenshot 2020-01-28 at 2.13.29 AM.png)

![Screenshot 2020-01-28 at 2.13.42 AM](/Users/apple/Desktop/Screenshot 2020-01-28 at 2.13.42 AM.png)



**So now that we have reached the final step lets recap everything that we have done in this exercise:-**

- We first enabled binlogs on mysql database.
- We created the required services in our local for the application using docker. 
- Next we created a connector using debezium-kafka connect which would listen to the creates/updates in our mysql database and push the changes to kafka.
- We wrote a kafka streams application that would listen to the kafka events in real time and create aggregated views.
- We created an index on elasticsearch using RestHighLevelClient.
- We wrote another listener using kafka streams that listens to individual kafka events and pushed in the updates.

### **Running the codebase in your local. :**

The code is available at https://github.com/rohan-mudaliar/big_data_pocs.git.

The project of interest is **kafkastreams-elasticsearch-demo** . Once you have taken a pull of the code, next step is to do a maven build of the same.

**SQL setup:**

We need to create the three tables wms_demo,order_demo and logistics_demo in three different databases. The sql scripts for the same are present in the project and given below.

**Create Scripts:**

```sql
CREATE TABLE `wms_demo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `courier` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_qty` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_localtion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `shipment_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE `order_demo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_date` datetime(6) DEFAULT NULL,
  `order_description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

```sql
CREATE TABLE `logistics_demo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `wmsrecord_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `item_type` datetime(6) DEFAULT NULL,
  `shipment_cost` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `courier` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Note: All the three tables are created in different databases.**

**Insert Scripts:**

```sql
INSERT INTO `logistics_demo` (`id`, `wmsrecord_id`, `item_type`, `shipment_cost`, `created_at`, `courier`, `order_id`)
VALUES(2,'1','0000-00-00 00:00:00.000000','24.5','2020-02-04 00:04:05.000000','FETCHR','1');

```

```sql
INSERT INTO `order_demo` (`id`, `order_id`, `order_date`, `order_description`, `order_value`, `customer_address`, `created_at`)
VALUES
	(1,'1','2020-02-02 12:23:25.000000','Apple Iphone X Black-128 GB','Rs 52755','Jotaro Kujo,No 1,2nd cross,RK Puram, Bangalore -5600103','2020-02-02 12:23:25.000000');
```

```sql
INSERT INTO `wms_demo` (`id`, `order_id`, `item_type`, `courier`, `item_qty`, `item_localtion`, `shipment_id`, `created_at`)
VALUES(1,'1','electronics','DHL','2','RACK12BA3','2','2020-02-03 12:28:44.000000');
```



For the purpose of poc, I have just 1 single order with 1 record in wms table and 1 record in logistics table. You can use the insert statements to add more records in your local.



**RUNNING THE CODE:**

Once you have your sql setup, to setup infrastructure follow the below steps:-

1. edit '**ADVERTISED_HOST_NAME**' in _docker-compose.yml_ to your machine's IP
2. run '`docker-compose up -d`'
3. run `curl -H "Accept:application/json" localhost:8083/` to check if kafka connect is up 
4. Create a connector in your local(refer to step **Create a mysql connector using kafka connect**.
5. Once Kafka connect is up `curl -H "Accept:application/json" localhost:8083/connectors/` to check if your connector is up
6. run '`kafka-topics --zookeeper localhost:2181 --list`' to get list of created topics
7. read topics using '`kafka-console-consumer --bootstrap-server <MACHINE.IP.ADDRESS>:9092 --topic <TOPIC.NAME> --from-beginning`'



NOTE:  
1. The user we use for the connection ought to have **RELOAD** and **REPLICATION** privileges. 
2. If any of your docker containers is unexpectedly shutting down, one of the most probably reason is that docker is running out of memory. 
  Follow below steps to rectify that:

    a) Open docker desktop preferences

    b) Go to advanced

    c) Set the slider to **4GB RAM**

Once the infrastructure is setup got to the terminal and type the below command.

![Screenshot 2020-02-12 at 9.09.14 AM](/Users/apple/Desktop/Screenshot 2020-02-12 at 9.09.14 AM.png)

This will run the application and will create the initial index in elasticsearch. Next step is to check for updates.



**Further Scope:**

- Currently in this project, we have worked only on the data engineering aspect of the project building a data pipeline, the final report required by business would require some analytics work to be done. 
- Currently I have worked on a just three tables, this can be extended to multiple tables and we can create complex aggregate views.
- One thing with this tech stack is there is no ML done, we can use the capabilities of spark MLlib in java to do ml tasks if required in java.