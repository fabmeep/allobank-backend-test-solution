package com.fabmeep.splitbill;

import com.fabmeep.splitbill.dto.AddExpenseRequest;
import com.fabmeep.splitbill.dto.CreateGroupRequest;
import com.fabmeep.splitbill.dto.ExpenseSplitItemRequest;
import com.fabmeep.splitbill.enums.SplitType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SplitbillIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End flow: Create group -> Add multiple expenses with various split types -> Retrieve settlement")
    void testCompleteSplitBillFlow() throws Exception {
        // 1. Create Group
        CreateGroupRequest createGroup = new CreateGroupRequest(
            "Road Trip 2026",
            "Weekend getaway with friends",
            List.of("Alice", "Bob", "Charlie")
        );

        MvcResult groupResult = mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createGroup)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        JsonNode groupJson = objectMapper.readTree(groupResult.getResponse().getContentAsString());
        UUID groupId = UUID.fromString(groupJson.get("data").get("id").asText());
        JsonNode participants = groupJson.get("data").get("participants");

        UUID aliceId = null;
        UUID bobId = null;
        UUID charlieId = null;

        for (JsonNode p : participants) {
            String name = p.get("name").asText();
            UUID id = UUID.fromString(p.get("id").asText());
            if ("Alice".equalsIgnoreCase(name)) aliceId = id;
            if ("Bob".equalsIgnoreCase(name)) bobId = id;
            if ("Charlie".equalsIgnoreCase(name)) charlieId = id;
        }

        assertThat(aliceId).isNotNull();
        assertThat(bobId).isNotNull();
        assertThat(charlieId).isNotNull();

        // 2. Add Expense 1: Alice pays $120.00 for all three (EQUAL: $40.00 each)
        AddExpenseRequest exp1 = new AddExpenseRequest(
            "Gasoline & Tolls",
            new BigDecimal("120.00"),
            aliceId,
            SplitType.EQUAL,
            null,
            null
        );

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp1)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.amount").value(120.00));

        // 3. Add Expense 2: Bob pays $60.00 for Charlie only (EXACT: $60.00)
        AddExpenseRequest exp2 = new AddExpenseRequest(
            "Theme Park Ticket for Charlie",
            new BigDecimal("60.00"),
            bobId,
            SplitType.EXACT,
            null,
            List.of(new ExpenseSplitItemRequest(charlieId, new BigDecimal("60.00"), null))
        );

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));

        // 4. Add Expense 3: Charlie pays $100.00 (PERCENTAGE: 50% Alice ($50.00), 50% Bob ($50.00))
        AddExpenseRequest exp3 = new AddExpenseRequest(
            "Souvenirs",
            new BigDecimal("100.00"),
            charlieId,
            SplitType.PERCENTAGE,
            null,
            List.of(
                new ExpenseSplitItemRequest(aliceId, null, new BigDecimal("50.00")),
                new ExpenseSplitItemRequest(bobId, null, new BigDecimal("50.00"))
            )
        );

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exp3)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true));

        // Total Expenses: 120 + 60 + 100 = 280.00
        // Alice: Paid 120, Share: 40 (gas) + 50 (souvenirs) = 90 -> Net +30.00
        // Bob: Paid 60, Share: 40 (gas) + 50 (souvenirs) = 90 -> Net -30.00
        // Charlie: Paid 100, Share: 40 (gas) + 60 (ticket) = 100 -> Net 0.00

        // 5. Retrieve Settlement Summary
        MvcResult settlementResult = mockMvc.perform(get("/api/v1/groups/{groupId}/settlements", groupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.groupId").value(groupId.toString()))
            .andExpect(jsonPath("$.data.groupName").value("Road Trip 2026"))
            .andExpect(jsonPath("$.data.totalExpenses").value(280.00))
            .andExpect(jsonPath("$.data.service_charge_pct").value(0))
            .andExpect(jsonPath("$.data.service_charge_amount").value(0.00))
            .andExpect(jsonPath("$.data.settlements").isArray())
            .andExpect(jsonPath("$.data.settlements.length()").value(1))
            .andExpect(jsonPath("$.data.settlements[0].fromParticipantName").value("Bob"))
            .andExpect(jsonPath("$.data.settlements[0].toParticipantName").value("Alice"))
            .andExpect(jsonPath("$.data.settlements[0].amount").value(30.00))
            .andReturn();

        JsonNode settlementJson = objectMapper.readTree(settlementResult.getResponse().getContentAsString());
        assertThat(settlementJson.get("data").get("settlements").get(0).get("amount").asDouble()).isEqualTo(30.00);
    }
}
