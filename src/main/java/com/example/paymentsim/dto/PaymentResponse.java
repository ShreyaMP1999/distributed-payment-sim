package com.example.paymentsim.dto;

import com.example.paymentsim.entity.Payment;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    String idempotencyKey,
    UUID payerAccountId,
    UUID payeeAccountId,
    long amountCents,
    Payment.Status status
) {}
