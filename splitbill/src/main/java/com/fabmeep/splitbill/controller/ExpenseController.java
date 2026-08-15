package com.fabmeep.splitbill.controller;

import com.fabmeep.splitbill.dto.AddExpenseRequest;
import com.fabmeep.splitbill.dto.ApiResponse;
import com.fabmeep.splitbill.dto.ExpenseResponse;
import com.fabmeep.splitbill.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> addExpense(
        @PathVariable UUID groupId,
        @Valid @RequestBody AddExpenseRequest request
    ) {
        ExpenseResponse response = expenseService.addExpense(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Expense added successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getExpenses(@PathVariable UUID groupId) {
        List<ExpenseResponse> responses = expenseService.getExpensesByGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
        @PathVariable UUID groupId,
        @PathVariable UUID expenseId
    ) {
        ExpenseResponse response = expenseService.getExpense(groupId, expenseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
