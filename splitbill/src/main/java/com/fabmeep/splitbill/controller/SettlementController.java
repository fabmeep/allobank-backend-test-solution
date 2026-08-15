package com.fabmeep.splitbill.controller;

import com.fabmeep.splitbill.dto.ApiResponse;
import com.fabmeep.splitbill.dto.SettlementSummaryResponse;
import com.fabmeep.splitbill.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping({"/settlements", "/settlement"})
    public ResponseEntity<ApiResponse<SettlementSummaryResponse>> getSettlement(@PathVariable UUID groupId) {
        SettlementSummaryResponse response = settlementService.getSettlementSummary(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
