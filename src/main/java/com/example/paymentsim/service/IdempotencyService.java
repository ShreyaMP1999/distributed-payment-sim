package com.example.paymentsim.service;

import com.example.paymentsim.entity.Payment;
import com.example.paymentsim.exception.ConflictException;
import com.example.paymentsim.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

/** Validates replay against durable, committed payment records. */
@Service
public class IdempotencyService {
  private final PaymentRepository payments;

  public IdempotencyService(PaymentRepository payments) {
    this.payments = payments;
  }

  public Optional<Payment> findCompleted(String key, String requestHash) {
    var existing = payments.findByIdempotencyKey(key);
    existing.ifPresent(payment -> {
      if (!payment.getRequestHash().equals(requestHash)) {
        throw new ConflictException("Idempotency-Key reuse with different request payload");
      }
      if (payment.getStatus() != Payment.Status.COMPLETED) {
        throw new ConflictException("Payment is not completed. Retry later.");
      }
    });
    return existing;
  }
}
