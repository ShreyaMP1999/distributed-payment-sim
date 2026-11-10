package com.example.paymentsim.service;

import com.example.paymentsim.entity.Payment;
import com.example.paymentsim.exception.ConflictException;
import com.example.paymentsim.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {
  final PaymentRepository repository = mock(PaymentRepository.class);
  final IdempotencyService service = new IdempotencyService(repository);
  Payment payment(Payment.Status status) {
    return new Payment(UUID.randomUUID(), "key", UUID.randomUUID(), UUID.randomUUID(), 100, status, "hash");
  }
  @Test void newKeyIsAvailable() {
    when(repository.findByIdempotencyKey("key")).thenReturn(Optional.empty());
    assertTrue(service.findCompleted("key", "hash").isEmpty());
  }
  @Test void committedPaymentIsReturned() {
    Payment p = payment(Payment.Status.COMPLETED);
    when(repository.findByIdempotencyKey("key")).thenReturn(Optional.of(p));
    assertSame(p, service.findCompleted("key", "hash").orElseThrow());
  }
  @Test void changedPayloadIsRejected() {
    when(repository.findByIdempotencyKey("key")).thenReturn(Optional.of(payment(Payment.Status.COMPLETED)));
    assertThrows(ConflictException.class, () -> service.findCompleted("key", "different"));
  }
  @Test void incompletePaymentIsRejected() {
    when(repository.findByIdempotencyKey("key")).thenReturn(Optional.of(payment(Payment.Status.PENDING)));
    assertThrows(ConflictException.class, () -> service.findCompleted("key", "hash"));
  }
}
