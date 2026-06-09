package com.tso.chat.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GenAiConfig {

  @Bean
  RestClient genAiRestClient(@Value("${genai.base-url}") String baseUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(10));
    factory.setReadTimeout(Duration.ofSeconds(60));
    return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }
}
