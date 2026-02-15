package com.example.paymentsim.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

  public enum Direction { DEBIT, CREDIT }

  @Id
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private UUID accountId;

  @Column(name = "payment_id", nullable = false)
  private UUID paymentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false, length = 10)
  private Direction direction;

  @Column(name = "amount_cents", nullable = false)
  private long amountCents;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  protected LedgerEntry() {}

  public LedgerEntry(UUID id, UUID accountId, UUID paymentId, Direction direction, long amountCents) {
    this.id = id;
    this.accountId = accountId;
    this.paymentId = paymentId;
    this.direction = direction;
    this.amountCents = amountCents;
  }

  public UUID getId() { return id; }
}
