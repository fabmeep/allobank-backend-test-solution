package com.fabmeep.splitbill.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ParticipantBalanceDto(
    UUID participantId,
    String participantName,
    BigDecimal totalPaid,
    BigDecimal totalShare,
    BigDecimal netBalance
) {}
