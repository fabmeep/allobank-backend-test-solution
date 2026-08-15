package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.ParticipantBalanceDto;
import com.fabmeep.splitbill.dto.SettlementSummaryResponse;
import com.fabmeep.splitbill.dto.SettlementTransactionDto;
import com.fabmeep.splitbill.entity.BillGroup;
import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.entity.Participant;
import com.fabmeep.splitbill.enums.SplitType;
import com.fabmeep.splitbill.exception.ResourceNotFoundException;
import com.fabmeep.splitbill.repository.BillGroupRepository;
import com.fabmeep.splitbill.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private BillGroupRepository groupRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SettlementCalculator settlementCalculator;

    @Mock
    private PersonalizationService personalizationService;

    @InjectMocks
    private SettlementService settlementService;

    private UUID groupId;
    private BillGroup group;
    private Participant alice;
    private Participant bob;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        group = new BillGroup("Test Group", "Desc");
        group.setId(groupId);

        alice = new Participant("Alice", group);
        alice.setId(UUID.randomUUID());

        bob = new Participant("Bob", group);
        bob.setId(UUID.randomUUID());

        group.setParticipants(List.of(alice, bob));
    }

    @Test
    @DisplayName("Should assemble settlement summary with balances, transactions, and personalization fields")
    void testGetSettlementSummarySuccess() {
        List<Expense> expenses = List.of(new Expense("Lunch", new BigDecimal("100.00"), alice, group, SplitType.EQUAL));

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));
        when(expenseRepository.findByGroupIdWithDetails(groupId)).thenReturn(expenses);
        when(settlementCalculator.calculateTotalExpenses(expenses)).thenReturn(new BigDecimal("100.00"));

        List<ParticipantBalanceDto> mockBalances = List.of(
            new ParticipantBalanceDto(alice.getId(), "Alice", new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
            new ParticipantBalanceDto(bob.getId(), "Bob", BigDecimal.ZERO, new BigDecimal("50.00"), new BigDecimal("-50.00"))
        );
        when(settlementCalculator.calculateBalances(group.getParticipants(), expenses)).thenReturn(mockBalances);

        List<SettlementTransactionDto> mockTx = List.of(
            new SettlementTransactionDto(bob.getId(), "Bob", alice.getId(), "Alice", new BigDecimal("50.00"))
        );
        when(settlementCalculator.calculateSettlements(mockBalances)).thenReturn(mockTx);

        when(personalizationService.getServiceChargePct()).thenReturn(0);
        when(personalizationService.calculateServiceChargeAmount(new BigDecimal("100.00"))).thenReturn(new BigDecimal("0.00"));

        SettlementSummaryResponse response = settlementService.getSettlementSummary(groupId);

        assertThat(response).isNotNull();
        assertThat(response.groupId()).isEqualTo(groupId);
        assertThat(response.groupName()).isEqualTo("Test Group");
        assertThat(response.totalExpenses()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.serviceChargePct()).isEqualTo(0);
        assertThat(response.serviceChargeAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(response.participantBalances()).hasSize(2);
        assertThat(response.settlements()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when group not found for settlement")
    void testGetSettlementSummaryNotFound() {
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.getSettlementSummary(groupId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Bill group not found");
    }
}
