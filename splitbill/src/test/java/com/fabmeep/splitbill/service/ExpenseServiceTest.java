package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.AddExpenseRequest;
import com.fabmeep.splitbill.dto.ExpenseResponse;
import com.fabmeep.splitbill.dto.ExpenseSplitItemRequest;
import com.fabmeep.splitbill.entity.BillGroup;
import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.entity.Participant;
import com.fabmeep.splitbill.enums.SplitType;
import com.fabmeep.splitbill.exception.BadRequestException;
import com.fabmeep.splitbill.exception.ResourceNotFoundException;
import com.fabmeep.splitbill.repository.BillGroupRepository;
import com.fabmeep.splitbill.repository.ExpenseRepository;
import com.fabmeep.splitbill.repository.ParticipantRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private BillGroupRepository groupRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID groupId;
    private BillGroup group;
    private Participant alice;
    private Participant bob;
    private Participant charlie;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        group = new BillGroup("Test Group", "Desc");
        group.setId(groupId);

        alice = new Participant("Alice", group);
        alice.setId(UUID.randomUUID());

        bob = new Participant("Bob", group);
        bob.setId(UUID.randomUUID());

        charlie = new Participant("Charlie", group);
        charlie.setId(UUID.randomUUID());

        group.setParticipants(List.of(alice, bob, charlie));
    }

    @Test
    @DisplayName("Should add expense with EQUAL split among all participants including remainder cents distribution")
    void testAddEqualSplitExpense() {
        // $100.00 split among 3 people -> 33.34, 33.33, 33.33 (Sum = 100.00)
        AddExpenseRequest request = new AddExpenseRequest(
            "Team Lunch",
            new BigDecimal("100.00"),
            alice.getId(),
            SplitType.EQUAL,
            null,
            null
        );

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ExpenseResponse response = expenseService.addExpense(groupId, request);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.payer().name()).isEqualTo("Alice");
        assertThat(response.splits()).hasSize(3);

        BigDecimal sumOfSplits = response.splits().stream()
            .map(s -> s.amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumOfSplits).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Should add expense with EXACT split")
    void testAddExactSplitExpense() {
        AddExpenseRequest request = new AddExpenseRequest(
            "Dinner",
            new BigDecimal("80.00"),
            alice.getId(),
            SplitType.EXACT,
            null,
            List.of(
                new ExpenseSplitItemRequest(alice.getId(), new BigDecimal("30.00"), null),
                new ExpenseSplitItemRequest(bob.getId(), new BigDecimal("50.00"), null)
            )
        );

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ExpenseResponse response = expenseService.addExpense(groupId, request);

        assertThat(response.splits()).hasSize(2);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("Should throw BadRequestException if EXACT split sum does not match total amount")
    void testExactSplitSumMismatch() {
        AddExpenseRequest request = new AddExpenseRequest(
            "Dinner",
            new BigDecimal("80.00"),
            alice.getId(),
            SplitType.EXACT,
            null,
            List.of(
                new ExpenseSplitItemRequest(alice.getId(), new BigDecimal("30.00"), null),
                new ExpenseSplitItemRequest(bob.getId(), new BigDecimal("40.00"), null) // Sum = 70 != 80
            )
        );

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("must equal the total expense amount");
    }

    @Test
    @DisplayName("Should add expense with PERCENTAGE split")
    void testAddPercentageSplitExpense() {
        AddExpenseRequest request = new AddExpenseRequest(
            "Hotel",
            new BigDecimal("200.00"),
            alice.getId(),
            SplitType.PERCENTAGE,
            null,
            List.of(
                new ExpenseSplitItemRequest(alice.getId(), null, new BigDecimal("50.00")),
                new ExpenseSplitItemRequest(bob.getId(), null, new BigDecimal("30.00")),
                new ExpenseSplitItemRequest(charlie.getId(), null, new BigDecimal("20.00"))
            )
        );

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ExpenseResponse response = expenseService.addExpense(groupId, request);

        assertThat(response.splits()).hasSize(3);
        BigDecimal sum = response.splits().stream().map(s -> s.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Should throw BadRequestException if PERCENTAGE split does not sum to 100%")
    void testPercentageSplitNotHundred() {
        AddExpenseRequest request = new AddExpenseRequest(
            "Hotel",
            new BigDecimal("200.00"),
            alice.getId(),
            SplitType.PERCENTAGE,
            null,
            List.of(
                new ExpenseSplitItemRequest(alice.getId(), null, new BigDecimal("50.00")),
                new ExpenseSplitItemRequest(bob.getId(), null, new BigDecimal("40.00")) // Sum = 90%
            )
        );

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Split percentages must sum to 100%");
    }

    @Test
    @DisplayName("Should throw BadRequestException if payer is not in group")
    void testPayerNotInGroup() {
        UUID nonMemberId = UUID.randomUUID();
        AddExpenseRequest request = new AddExpenseRequest(
            "Lunch",
            new BigDecimal("50.00"),
            nonMemberId,
            SplitType.EQUAL,
            null,
            null
        );

        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Payer does not belong to this bill group");
    }

    @Test
    @DisplayName("Should get expenses for group")
    void testGetExpensesForGroup() {
        when(groupRepository.existsById(groupId)).thenReturn(true);
        Expense exp = new Expense("Test", new BigDecimal("10.00"), alice, group, SplitType.EQUAL);
        exp.setId(UUID.randomUUID());
        when(expenseRepository.findByGroupIdWithDetails(groupId)).thenReturn(List.of(exp));

        List<ExpenseResponse> responses = expenseService.getExpensesByGroup(groupId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).description()).isEqualTo("Test");
    }

    @Test
    @DisplayName("Should get single expense by id")
    void testGetSingleExpense() {
        UUID expId = UUID.randomUUID();
        Expense exp = new Expense("Test Expense", new BigDecimal("45.00"), alice, group, SplitType.EQUAL);
        exp.setId(expId);

        when(expenseRepository.findByIdAndGroupIdWithDetails(expId, groupId)).thenReturn(Optional.of(exp));

        ExpenseResponse response = expenseService.getExpense(groupId, expId);

        assertThat(response.id()).isEqualTo(expId);
        assertThat(response.description()).isEqualTo("Test Expense");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when single expense not found")
    void testGetSingleExpenseNotFound() {
        UUID expId = UUID.randomUUID();
        when(expenseRepository.findByIdAndGroupIdWithDetails(expId, groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpense(groupId, expId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Expense not found with id");
    }

    @Test
    @DisplayName("Should throw BadRequestException if amount is zero or negative")
    void testInvalidAmount() {
        AddExpenseRequest req = new AddExpenseRequest("Free lunch", BigDecimal.ZERO, alice.getId(), SplitType.EQUAL, null, null);
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Expense amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw BadRequestException if participant in equal split is not in group")
    void testEqualSplitParticipantNotInGroup() {
        UUID unknownId = UUID.randomUUID();
        AddExpenseRequest req = new AddExpenseRequest("Lunch", new BigDecimal("50.00"), alice.getId(), SplitType.EQUAL, List.of(unknownId), null);
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("does not belong to this bill group");
    }

    @Test
    @DisplayName("Should throw BadRequestException if exact split items is null or empty")
    void testExactSplitItemsEmpty() {
        AddExpenseRequest req = new AddExpenseRequest("Lunch", new BigDecimal("50.00"), alice.getId(), SplitType.EXACT, null, List.of());
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Exact split items are required");
    }

    @Test
    @DisplayName("Should throw BadRequestException if percentage split items is null or empty")
    void testPercentageSplitItemsEmpty() {
        AddExpenseRequest req = new AddExpenseRequest("Lunch", new BigDecimal("50.00"), alice.getId(), SplitType.PERCENTAGE, null, null);
        when(groupRepository.findByIdWithParticipants(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> expenseService.addExpense(groupId, req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Split items with percentages are required");
    }
}
