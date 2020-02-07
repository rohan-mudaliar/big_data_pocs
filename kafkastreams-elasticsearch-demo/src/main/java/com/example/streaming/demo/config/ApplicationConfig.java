package com.example.streaming.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfig {
  @Value("${rest.template.connection.timeout:1000}")
  private Integer connectionTimeout;

  @Value("${rest.template.connection.request.timeout:1000}")
  private Integer connectionRequestTimeout;

  @Value("${rest.template.socket.timeout:30000}")
  private Integer socketTimeout;

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate(getClientHttpRequestFactory());
  }
  private ClientHttpRequestFactory getClientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    httpComponentsClientHttpRequestFactory.setReadTimeout(socketTimeout);
    httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
    httpComponentsClientHttpRequestFactory.setConnectTimeout(connectionTimeout);
    return httpComponentsClientHttpRequestFactory;
  }
}
