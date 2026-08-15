package com.fabmeep.splitbill.controller;

import com.fabmeep.splitbill.dto.AddExpenseRequest;
import com.fabmeep.splitbill.dto.ExpenseResponse;
import com.fabmeep.splitbill.dto.ParticipantResponse;
import com.fabmeep.splitbill.enums.SplitType;
import com.fabmeep.splitbill.exception.GlobalExceptionHandler;
import com.fabmeep.splitbill.service.ExpenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@Import(GlobalExceptionHandler.class)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    @Test
    @DisplayName("POST /api/v1/groups/{groupId}/expenses - 201 Created on valid expense")
    void testAddExpense() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();

        AddExpenseRequest request = new AddExpenseRequest(
            "Dinner",
            new BigDecimal("60.00"),
            payerId,
            SplitType.EQUAL,
            null,
            null
        );

        ExpenseResponse response = new ExpenseResponse(
            expenseId,
            "Dinner",
            new BigDecimal("60.00"),
            new ParticipantResponse(payerId, "Alice"),
            SplitType.EQUAL,
            List.of(),
            Instant.now()
        );

        when(expenseService.addExpense(eq(groupId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/groups/{groupId}/expenses", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(expenseId.toString()))
            .andExpect(jsonPath("$.data.description").value("Dinner"))
            .andExpect(jsonPath("$.data.amount").value(60.00));
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId}/expenses - 200 OK")
    void testGetExpenses() throws Exception {
        UUID groupId = UUID.randomUUID();
        ExpenseResponse exp = new ExpenseResponse(
            UUID.randomUUID(),
            "Coffee",
            new BigDecimal("15.00"),
            new ParticipantResponse(UUID.randomUUID(), "Bob"),
            SplitType.EQUAL,
            List.of(),
            Instant.now()
        );

        when(expenseService.getExpensesByGroup(groupId)).thenReturn(List.of(exp));

        mockMvc.perform(get("/api/v1/groups/{groupId}/expenses", groupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/groups/{groupId}/expenses/{expenseId} - 200 OK")
    void testGetSingleExpense() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        ExpenseResponse exp = new ExpenseResponse(
            expenseId,
            "Lunch",
            new BigDecimal("30.00"),
            new ParticipantResponse(UUID.randomUUID(), "Alice"),
            SplitType.EQUAL,
            List.of(),
            Instant.now()
        );

        when(expenseService.getExpense(groupId, expenseId)).thenReturn(exp);

        mockMvc.perform(get("/api/v1/groups/{groupId}/expenses/{expenseId}", groupId, expenseId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(expenseId.toString()))
            .andExpect(jsonPath("$.data.description").value("Lunch"));
    }
}
