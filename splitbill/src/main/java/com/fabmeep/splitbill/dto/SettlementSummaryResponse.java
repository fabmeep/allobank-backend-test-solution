package com.fabmeep.splitbill.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SettlementSummaryResponse(
    UUID groupId,
    String groupName,
    BigDecimal totalExpenses,
    @JsonProperty("service_charge_pct")
    int serviceChargePct,
    @JsonProperty("service_charge_amount")
    BigDecimal serviceChargeAmount,
    List<ParticipantBalanceDto> participantBalances,
    List<SettlementTransactionDto> settlements
) {}
