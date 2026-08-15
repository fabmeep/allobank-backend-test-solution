package com.fabmeep.splitbill.dto;

import com.fabmeep.splitbill.entity.ExpenseSplit;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSplitResponse(
    UUID participantId,
    String participantName,
    BigDecimal amount,
    BigDecimal percentage
) {
    public static ExpenseSplitResponse fromEntity(ExpenseSplit split) {
        return new ExpenseSplitResponse(
            split.getParticipant().getId(),
            split.getParticipant().getName(),
            split.getAmount(),
            split.getPercentage()
        );
    }
}
