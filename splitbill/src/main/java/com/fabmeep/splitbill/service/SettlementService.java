package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.ParticipantBalanceDto;
import com.fabmeep.splitbill.dto.SettlementSummaryResponse;
import com.fabmeep.splitbill.dto.SettlementTransactionDto;
import com.fabmeep.splitbill.entity.BillGroup;
import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.entity.Participant;
import com.fabmeep.splitbill.exception.ResourceNotFoundException;
import com.fabmeep.splitbill.repository.BillGroupRepository;
import com.fabmeep.splitbill.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SettlementService {

    private final BillGroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementCalculator settlementCalculator;
    private final PersonalizationService personalizationService;

    public SettlementService(
            BillGroupRepository groupRepository,
            ExpenseRepository expenseRepository,
            SettlementCalculator settlementCalculator,
            PersonalizationService personalizationService) {
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.settlementCalculator = settlementCalculator;
        this.personalizationService = personalizationService;
    }

    @Transactional(readOnly = true)
    public SettlementSummaryResponse getSettlementSummary(UUID groupId) {
        BillGroup group = groupRepository.findByIdWithParticipants(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill group not found with id: " + groupId));

        List<Participant> participants = group.getParticipants();
        List<Expense> expenses = expenseRepository.findByGroupIdWithDetails(groupId);

        BigDecimal totalExpenses = settlementCalculator.calculateTotalExpenses(expenses);
        List<ParticipantBalanceDto> balances = settlementCalculator.calculateBalances(participants, expenses);
        List<SettlementTransactionDto> settlements = settlementCalculator.calculateSettlements(balances);

        int serviceChargePct = personalizationService.getServiceChargePct();
        BigDecimal serviceChargeAmount = personalizationService.calculateServiceChargeAmount(totalExpenses);

        return new SettlementSummaryResponse(
                group.getId(),
                group.getName(),
                totalExpenses,
                serviceChargePct,
                serviceChargeAmount,
                balances,
                settlements);
    }
}
