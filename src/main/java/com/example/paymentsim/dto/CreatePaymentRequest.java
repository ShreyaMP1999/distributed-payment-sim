package com.example.paymentsim.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull UUID payerAccountId,
    @NotNull UUID payeeAccountId,
    @Positive long amountCents
) {}
