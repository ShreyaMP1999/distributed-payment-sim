package com.example.paymentsim.controller;

import com.example.paymentsim.dto.CreateAccountRequest;
import com.example.paymentsim.dto.CreatePaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIT {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;

  @Test
  void createPaymentRequiresIdempotencyKey() throws Exception {
    var req = new CreatePaymentRequest(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 10);
    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }
}
