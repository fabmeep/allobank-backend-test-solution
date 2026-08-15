package com.fabmeep.splitbill.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementTransactionDto(
    UUID fromParticipantId,
    String fromParticipantName,
    UUID toParticipantId,
    String toParticipantName,
    BigDecimal amount
) {}
