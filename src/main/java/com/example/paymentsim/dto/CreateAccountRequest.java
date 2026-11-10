package com.example.paymentsim.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(
    @NotBlank @Size(max = 120) String ownerName,
    @PositiveOrZero long initialBalanceCents
) {}

