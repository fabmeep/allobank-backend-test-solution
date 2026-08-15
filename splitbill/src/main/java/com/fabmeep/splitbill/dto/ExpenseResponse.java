package com.fabmeep.splitbill.dto;

import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.enums.SplitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    String description,
    BigDecimal amount,
    ParticipantResponse payer,
    SplitType splitType,
    List<ExpenseSplitResponse> splits,
    Instant createdAt
) {
    public static ExpenseResponse fromEntity(Expense expense) {
        List<ExpenseSplitResponse> splitResponses = expense.getSplits() != null
            ? expense.getSplits().stream().map(ExpenseSplitResponse::fromEntity).toList()
            : List.of();
        return new ExpenseResponse(
            expense.getId(),
            expense.getDescription(),
            expense.getAmount(),
            ParticipantResponse.fromEntity(expense.getPayer()),
            expense.getSplitType(),
            splitResponses,
            expense.getCreatedAt()
        );
    }
}
