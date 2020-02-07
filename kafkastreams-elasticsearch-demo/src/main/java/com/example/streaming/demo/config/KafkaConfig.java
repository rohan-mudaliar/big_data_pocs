package com.example.streaming.demo.config;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
	
	@Value("${kafka.bootstrap.servers}")
	private String KafkabootStrapServer;
	public  Properties populateKafkConfigMap() {
		Properties props = new Properties();
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "new-test-application1");
		// "192.168.0.241:9092"
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkabootStrapServer);
		props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		return props;
	}
}
