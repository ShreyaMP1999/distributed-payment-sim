package com.example.paymentsim.controller;

import com.example.paymentsim.dto.CreatePaymentRequest;
import com.example.paymentsim.dto.PaymentResponse;
import com.example.paymentsim.exception.BadRequestException;
import com.example.paymentsim.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentResponse create(
      @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
      @Valid @RequestBody CreatePaymentRequest req
  ) {
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.trim().length() > 120) {
      throw new BadRequestException("Idempotency-Key must contain 1 to 120 characters");
    }
    return paymentService.create(idempotencyKey.trim(), req);
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@PathVariable UUID id) {
    return paymentService.get(id);
  }
}

