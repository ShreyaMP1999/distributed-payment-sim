package com.example.paymentsim.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

  @Id
  private UUID id;

  @Column(name = "owner_name", nullable = false, length = 120)
  private String ownerName;

  @Column(name = "balance_cents", nullable = false)
  private long balanceCents;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Account() {}

  public Account(UUID id, String ownerName, long balanceCents) {
    this.id = id;
    this.ownerName = ownerName;
    this.balanceCents = balanceCents;
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = Instant.now();
  }

  public void deposit(long amountCents) {
    this.balanceCents = Math.addExact(this.balanceCents, amountCents);
  }

  public void withdraw(long amountCents) {
    if (this.balanceCents < amountCents) {
      throw new IllegalStateException("Insufficient funds");
    }
    this.balanceCents = Math.subtractExact(this.balanceCents, amountCents);
  }

  public UUID getId() { return id; }
  public String getOwnerName() { return ownerName; }
  public long getBalanceCents() { return balanceCents; }
  public long getVersion() { return version; }
}
