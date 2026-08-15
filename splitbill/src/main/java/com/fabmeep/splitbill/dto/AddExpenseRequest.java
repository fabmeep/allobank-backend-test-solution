package com.fabmeep.splitbill.dto;

import com.fabmeep.splitbill.enums.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddExpenseRequest(
    @NotBlank(message = "Expense description is required")
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,

    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than 0")
    BigDecimal amount,

    @NotNull(message = "Payer ID is required")
    UUID payerId,

    SplitType splitType,

    List<UUID> participantIds,

    List<ExpenseSplitItemRequest> splits
) {}
