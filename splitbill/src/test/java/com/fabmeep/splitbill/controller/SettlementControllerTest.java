package com.fabmeep.splitbill.controller;

import com.fabmeep.splitbill.dto.ParticipantBalanceDto;
import com.fabmeep.splitbill.dto.SettlementSummaryResponse;
import com.fabmeep.splitbill.dto.SettlementTransactionDto;
import com.fabmeep.splitbill.exception.GlobalExceptionHandler;
import com.fabmeep.splitbill.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
@Import(GlobalExceptionHandler.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettlementService settlementService;

    @Test
    @DisplayName("GET /api/v1/groups/{groupId}/settlements - 200 OK with required personalization fields")
    void testGetSettlement() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();

        SettlementSummaryResponse response = new SettlementSummaryResponse(
            groupId,
            "Weekend Trip",
            new BigDecimal("150.00"),
            0,
            new BigDecimal("0.00"),
            List.of(
                new ParticipantBalanceDto(aliceId, "Alice", new BigDecimal("150.00"), new BigDecimal("75.00"), new BigDecimal("75.00")),
                new ParticipantBalanceDto(bobId, "Bob", BigDecimal.ZERO, new BigDecimal("75.00"), new BigDecimal("-75.00"))
            ),
            List.of(
                new SettlementTransactionDto(bobId, "Bob", aliceId, "Alice", new BigDecimal("75.00"))
            )
        );

        when(settlementService.getSettlementSummary(groupId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/groups/{groupId}/settlements", groupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.groupId").value(groupId.toString()))
            .andExpect(jsonPath("$.data.totalExpenses").value(150.00))
            .andExpect(jsonPath("$.data.service_charge_pct").value(0))
            .andExpect(jsonPath("$.data.service_charge_amount").value(0.00))
            .andExpect(jsonPath("$.data.participantBalances").isArray())
            .andExpect(jsonPath("$.data.settlements").isArray())
            .andExpect(jsonPath("$.data.settlements[0].fromParticipantName").value("Bob"))
            .andExpect(jsonPath("$.data.settlements[0].toParticipantName").value("Alice"))
            .andExpect(jsonPath("$.data.settlements[0].amount").value(75.00));
    }
}
