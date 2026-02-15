package com.example.paymentsim.dto;

import jakarta.validation.constraints.Positive;

public record DepositRequest(@Positive long amountCents) {}
