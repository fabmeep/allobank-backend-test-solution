package com.fabmeep.splitbill.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSplitItemRequest(
    @NotNull(message = "Participant ID is required")
    UUID participantId,

    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @Positive(message = "Percentage must be positive")
    BigDecimal percentage
) {}
