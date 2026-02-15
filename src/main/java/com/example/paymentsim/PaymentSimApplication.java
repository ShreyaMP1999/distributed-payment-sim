package com.example.paymentsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PaymentSimApplication {
  public static void main(String[] args) {
    SpringApplication.run(PaymentSimApplication.class, args);
  }
}
