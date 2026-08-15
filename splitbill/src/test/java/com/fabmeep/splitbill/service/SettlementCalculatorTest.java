package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.ParticipantBalanceDto;
import com.fabmeep.splitbill.dto.SettlementTransactionDto;
import com.fabmeep.splitbill.entity.BillGroup;
import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.entity.ExpenseSplit;
import com.fabmeep.splitbill.entity.Participant;
import com.fabmeep.splitbill.enums.SplitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementCalculatorTest {

    private SettlementCalculator calculator;
    private BillGroup group;
    private Participant alice;
    private Participant bob;
    private Participant charlie;
    private Participant david;

    @BeforeEach
    void setUp() {
        calculator = new SettlementCalculator();
        group = new BillGroup("Trip", "Test trip");
        group.setId(UUID.randomUUID());

        alice = new Participant("Alice", group);
        alice.setId(UUID.randomUUID());

        bob = new Participant("Bob", group);
        bob.setId(UUID.randomUUID());

        charlie = new Participant("Charlie", group);
        charlie.setId(UUID.randomUUID());

        david = new Participant("David", group);
        david.setId(UUID.randomUUID());

        group.setParticipants(List.of(alice, bob, charlie, david));
    }

    @Test
    @DisplayName("Should return empty settlements when there are no expenses")
    void testNoExpenses() {
        List<Participant> participants = List.of(alice, bob);
        List<Expense> expenses = List.of();

        BigDecimal total = calculator.calculateTotalExpenses(expenses);
        List<ParticipantBalanceDto> balances = calculator.calculateBalances(participants, expenses);
        List<SettlementTransactionDto> settlements = calculator.calculateSettlements(balances);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balances).hasSize(2);
        assertThat(balances).allMatch(b -> b.netBalance().compareTo(BigDecimal.ZERO) == 0);
        assertThat(settlements).isEmpty();
    }

    @Test
    @DisplayName("Should calculate simple 2-person settlement: Alice pays $100 for Alice and Bob equally")
    void testSimpleTwoPersonEqualSplit() {
        List<Participant> participants = List.of(alice, bob);

        Expense expense = new Expense("Dinner", new BigDecimal("100.00"), alice, group, SplitType.EQUAL);
        expense.setId(UUID.randomUUID());
        expense.setSplits(List.of(
            new ExpenseSplit(expense, alice, new BigDecimal("50.00")),
            new ExpenseSplit(expense, bob, new BigDecimal("50.00"))
        ));

        List<Expense> expenses = List.of(expense);

        BigDecimal total = calculator.calculateTotalExpenses(expenses);
        List<ParticipantBalanceDto> balances = calculator.calculateBalances(participants, expenses);
        List<SettlementTransactionDto> settlements = calculator.calculateSettlements(balances);

        assertThat(total).isEqualByComparingTo(new BigDecimal("100.00"));

        // Alice paid 100, share 50 -> net +50
        ParticipantBalanceDto aliceBal = balances.stream().filter(b -> b.participantId().equals(alice.getId())).findFirst().orElseThrow();
        assertThat(aliceBal.totalPaid()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(aliceBal.totalShare()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(aliceBal.netBalance()).isEqualByComparingTo(new BigDecimal("50.00"));

        // Bob paid 0, share 50 -> net -50
        ParticipantBalanceDto bobBal = balances.stream().filter(b -> b.participantId().equals(bob.getId())).findFirst().orElseThrow();
        assertThat(bobBal.totalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(bobBal.totalShare()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(bobBal.netBalance()).isEqualByComparingTo(new BigDecimal("-50.00"));

        // Settlement: Bob pays Alice $50.00
        assertThat(settlements).hasSize(1);
        SettlementTransactionDto tx = settlements.get(0);
        assertThat(tx.fromParticipantId()).isEqualTo(bob.getId());
        assertThat(tx.fromParticipantName()).isEqualTo("Bob");
        assertThat(tx.toParticipantId()).isEqualTo(alice.getId());
        assertThat(tx.toParticipantName()).isEqualTo("Alice");
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should simplify 3-person cyclic debts into minimal transactions")
    void testCyclicDebtsSimplification() {
        // Alice pays 90 for Alice, Bob, Charlie (30 each)
        // Bob pays 60 for Bob, Charlie (30 each)
        // Charlie pays 30 for Alice (30)
        List<Participant> participants = List.of(alice, bob, charlie);

        Expense exp1 = new Expense("Lunch", new BigDecimal("90.00"), alice, group, SplitType.EQUAL);
        exp1.setSplits(List.of(
            new ExpenseSplit(exp1, alice, new BigDecimal("30.00")),
            new ExpenseSplit(exp1, bob, new BigDecimal("30.00")),
            new ExpenseSplit(exp1, charlie, new BigDecimal("30.00"))
        ));

        Expense exp2 = new Expense("Taxi", new BigDecimal("60.00"), bob, group, SplitType.EQUAL);
        exp2.setSplits(List.of(
            new ExpenseSplit(exp2, bob, new BigDecimal("30.00")),
            new ExpenseSplit(exp2, charlie, new BigDecimal("30.00"))
        ));

        Expense exp3 = new Expense("Coffee", new BigDecimal("30.00"), charlie, group, SplitType.EXACT);
        exp3.setSplits(List.of(
            new ExpenseSplit(exp3, alice, new BigDecimal("30.00"))
        ));

        List<Expense> expenses = List.of(exp1, exp2, exp3);
        // Alice: Paid 90, Share 60 (30 lunch + 30 coffee) -> Net +30
        // Bob: Paid 60, Share 60 (30 lunch + 30 taxi) -> Net 0
        // Charlie: Paid 30, Share 60 (30 lunch + 30 taxi) -> Net -30

        List<ParticipantBalanceDto> balances = calculator.calculateBalances(participants, expenses);
        List<SettlementTransactionDto> settlements = calculator.calculateSettlements(balances);

        // Charlie should directly pay Alice $30.00, Bob has 0 transactions
        assertThat(settlements).hasSize(1);
        SettlementTransactionDto tx = settlements.get(0);
        assertThat(tx.fromParticipantId()).isEqualTo(charlie.getId());
        assertThat(tx.toParticipantId()).isEqualTo(alice.getId());
        assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Should handle 4-person complex multi-debt settlement with minimal transactions")
    void testComplexFourPersonSettlement() {
        // Alice pays 120 for Alice, Bob, Charlie, David (30 each)
        // Bob pays 80 for Charlie, David (40 each)
        List<Participant> participants = List.of(alice, bob, charlie, david);

        Expense exp1 = new Expense("Groceries", new BigDecimal("120.00"), alice, group, SplitType.EQUAL);
        exp1.setSplits(List.of(
            new ExpenseSplit(exp1, alice, new BigDecimal("30.00")),
            new ExpenseSplit(exp1, bob, new BigDecimal("30.00")),
            new ExpenseSplit(exp1, charlie, new BigDecimal("30.00")),
            new ExpenseSplit(exp1, david, new BigDecimal("30.00"))
        ));

        Expense exp2 = new Expense("Concert", new BigDecimal("80.00"), bob, group, SplitType.EQUAL);
        exp2.setSplits(List.of(
            new ExpenseSplit(exp2, charlie, new BigDecimal("40.00")),
            new ExpenseSplit(exp2, david, new BigDecimal("40.00"))
        ));

        List<Expense> expenses = List.of(exp1, exp2);

        // Alice: Paid 120, Share 30 -> Net +90
        // Bob: Paid 80, Share 30 -> Net +50
        // Charlie: Paid 0, Share 70 -> Net -70
        // David: Paid 0, Share 70 -> Net -70

        List<ParticipantBalanceDto> balances = calculator.calculateBalances(participants, expenses);
        List<SettlementTransactionDto> settlements = calculator.calculateSettlements(balances);

        // Sum of settlements must equal total net debt (140)
        BigDecimal totalSettled = settlements.stream()
            .map(SettlementTransactionDto::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalSettled).isEqualByComparingTo(new BigDecimal("140.00"));

        // Min transactions needed: 3 (since gcd/subsets: 70+70 = 90+50)
        assertThat(settlements.size()).isLessThanOrEqualTo(3);
    }
}
