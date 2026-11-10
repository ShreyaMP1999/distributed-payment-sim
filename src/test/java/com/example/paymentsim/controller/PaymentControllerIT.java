package com.example.paymentsim.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.paymentsim.repository.LedgerEntryRepository;
import com.example.paymentsim.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIT {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;
  @Autowired PaymentRepository payments;
  @Autowired LedgerEntryRepository ledger;

  String account(long balance) throws Exception {
    String response = mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"ownerName\":\"Test\",\"initialBalanceCents\":" + balance + "}"))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return om.readTree(response).get("id").asText();
  }
  String body(String payer, String payee, long amount) {
    return "{\"payerAccountId\":\""+payer+"\",\"payeeAccountId\":\""+payee+"\",\"amountCents\":"+amount+"}";
  }
  JsonNode balance(String id) throws Exception {
    return om.readTree(mvc.perform(get("/api/v1/accounts/"+id)).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }
  String transfer(String key, String body, int status) throws Exception {
    return mvc.perform(post("/api/v1/payments").header("Idempotency-Key",key)
        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(status))
        .andReturn().getResponse().getContentAsString();
  }
  @Test void duplicateReturnsSamePaymentAndFreshBalances() throws Exception {
    String payer=account(1000), payee=account(0), key=UUID.randomUUID().toString();
    balance(payer); // A read before a write must not leave a cached balance.
    long entries=ledger.count();
    String first=transfer(key,body(payer,payee,300),201);
    assertEquals(first,transfer(key,body(payer,payee,300),201));
    assertEquals(700,balance(payer).get("balanceCents").asLong());
    assertEquals(300,balance(payee).get("balanceCents").asLong());
    assertEquals(entries+2,ledger.count());
    String id=om.readTree(first).get("id").asText();
    mvc.perform(get("/api/v1/payments/"+id)).andExpect(status().isOk())
        .andExpect(jsonPath("status").value("COMPLETED"));
    transfer(key,body(payer,payee,301),409);
  }
  @Test void insufficientFundsRollsBackAndSameKeyCanRetry() throws Exception {
    String payer=account(100),payee=account(0),key=UUID.randomUUID().toString();
    long entries=ledger.count();
    transfer(key,body(payer,payee,200),400);
    assertTrue(payments.findByIdempotencyKey(key).isEmpty());
    assertEquals(entries,ledger.count());
    assertEquals(100,balance(payer).get("balanceCents").asLong());
    mvc.perform(post("/api/v1/accounts/"+payer+"/deposit").contentType(MediaType.APPLICATION_JSON)
        .content("{\"amountCents\":200}")).andExpect(status().isOk());
    transfer(key,body(payer,payee,200),201);
    assertEquals(100,balance(payer).get("balanceCents").asLong());
  }
  @Test void overflowRollsBackBothAccounts() throws Exception {
    String payer=account(100),payee=account(Long.MAX_VALUE),key=UUID.randomUUID().toString();
    transfer(key,body(payer,payee,1),400);
    assertEquals(100,balance(payer).get("balanceCents").asLong());
    assertEquals(Long.MAX_VALUE,balance(payee).get("balanceCents").asLong());
    assertTrue(payments.findByIdempotencyKey(key).isEmpty());
  }
  @Test void validatesHeadersIdentifiersAndBodies() throws Exception {
    String payer=account(100),payee=account(0), b=body(payer,payee,1);
    mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content(b))
        .andExpect(status().isBadRequest());
    transfer(" ",b,400);
    transfer("x".repeat(121),b,400);
    transfer(UUID.randomUUID().toString(),body(payer,payee,0),400);
    transfer(UUID.randomUUID().toString(),body(payer,payer,1),400);
    transfer(UUID.randomUUID().toString(),body(payer,UUID.randomUUID().toString(),1),404);
    transfer(UUID.randomUUID().toString(),"{",400);
    mvc.perform(get("/api/v1/accounts/not-a-uuid")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/accounts/"+UUID.randomUUID())).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"ownerName\":\"\",\"initialBalanceCents\":-1}"))
        .andExpect(status().isBadRequest());
  }
  @Test void concurrentDuplicateIsDebitedOnce() throws Exception {
    String payer=account(1000),payee=account(0),key=UUID.randomUUID().toString(),b=body(payer,payee,100);
    var pool=Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> call=()->mvc.perform(post("/api/v1/payments").header("Idempotency-Key",key)
          .contentType(MediaType.APPLICATION_JSON).content(b)).andReturn().getResponse().getStatus();
      for (var result:pool.invokeAll(List.of(call,call))) {
        int code=result.get(); assertTrue(code==201 || code==409,"Unexpected HTTP status: "+code);
      }
      transfer(key,b,201);
      assertEquals(900,balance(payer).get("balanceCents").asLong());
      assertEquals(100,balance(payee).get("balanceCents").asLong());
    } finally { pool.shutdownNow(); }
  }
}
