package com.example.paymentsim.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

  public enum Status { PENDING, COMPLETED, FAILED }

  @Id
  private UUID id;

  @Column(name = "idempotency_key", nullable = false, length = 120, unique = true)
  private String idempotencyKey;

  @Column(name = "payer_account_id", nullable = false)
  private UUID payerAccountId;

  @Column(name = "payee_account_id", nullable = false)
  private UUID payeeAccountId;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private Status status;

  @Column(name = "request_hash", nullable = false, length = 128)
  private String requestHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected Payment() {}

  public Payment(UUID id, String idempotencyKey, UUID payerAccountId, UUID payeeAccountId,
                 long amountCents, Status status, String requestHash) {
    this.id = id;
    this.idempotencyKey = idempotencyKey;
    this.payerAccountId = payerAccountId;
    this.payeeAccountId = payeeAccountId;
    this.amountCents = amountCents;
    this.status = status;
    this.requestHash = requestHash;
  }

  public UUID getId() { return id; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public UUID getPayerAccountId() { return payerAccountId; }
  public UUID getPayeeAccountId() { return payeeAccountId; }
  public long getAmountCents() { return amountCents; }
  public Status getStatus() { return status; }
  public String getRequestHash() { return requestHash; }

  public void markCompleted() { this.status = Status.COMPLETED; }
  public void markFailed() { this.status = Status.FAILED; }
}
