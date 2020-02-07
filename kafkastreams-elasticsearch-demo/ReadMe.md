1. edit '**ADVERTISED_HOST_NAME**' in _docker-compose.yml_ to your machine's IP
2. run '`docker-compose up -d`'
3. run `curl -H "Accept:application/json" localhost:8083/` to check if kafka connect is up 
4. run this curl to register a mysql connector

    `curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{
      "name": "wms-connector",
      "config": {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "tasks.max": "1",
        "database.hostname": "<DB.HOSTNAME>",
        "database.port": "3306",
        "database.user": "<DB.USERNAME>",
        "database.password": "<DB.PASSWORD>",
        "database.server.id": "1234",
        "database.server.name": "dp",
        "database.whitelist": "wms",
        "database.history.kafka.bootstrap.servers": "kafka:9092",
        "database.history.kafka.topic": "dp.wms"
      }
    }'`
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