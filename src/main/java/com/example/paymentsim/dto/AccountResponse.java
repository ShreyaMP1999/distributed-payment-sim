package com.example.paymentsim.dto;

import java.util.UUID;

public record AccountResponse(UUID id, String ownerName, long balanceCents) {}
