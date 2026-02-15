package com.example.paymentsim.config;

import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
  @Bean
  public SimpleKeyGenerator keyGenerator() {
    return new SimpleKeyGenerator();
  }
}
