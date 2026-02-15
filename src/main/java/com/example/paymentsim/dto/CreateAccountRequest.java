package com.example.paymentsim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(
    @NotBlank String ownerName,
    @PositiveOrZero long initialBalanceCents
) {}
