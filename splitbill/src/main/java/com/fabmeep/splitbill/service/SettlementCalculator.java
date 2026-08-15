package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.ParticipantBalanceDto;
import com.fabmeep.splitbill.dto.SettlementTransactionDto;
import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.entity.ExpenseSplit;
import com.fabmeep.splitbill.entity.Participant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SettlementCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public BigDecimal calculateTotalExpenses(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        }
        return expenses.stream()
            .map(Expense::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(SCALE, ROUNDING_MODE);
    }

    public List<ParticipantBalanceDto> calculateBalances(List<Participant> participants, List<Expense> expenses) {
        Map<UUID, BigDecimal> totalPaidMap = new HashMap<>();
        Map<UUID, BigDecimal> totalShareMap = new HashMap<>();

        for (Participant p : participants) {
            totalPaidMap.put(p.getId(), BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE));
            totalShareMap.put(p.getId(), BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE));
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                UUID payerId = expense.getPayer().getId();
                totalPaidMap.put(
                    payerId,
                    totalPaidMap.getOrDefault(payerId, BigDecimal.ZERO).add(expense.getAmount())
                );

                if (expense.getSplits() != null) {
                    for (ExpenseSplit split : expense.getSplits()) {
                        UUID participantId = split.getParticipant().getId();
                        totalShareMap.put(
                            participantId,
                            totalShareMap.getOrDefault(participantId, BigDecimal.ZERO).add(split.getAmount())
                        );
                    }
                }
            }
        }

        List<ParticipantBalanceDto> balances = new ArrayList<>();
        for (Participant p : participants) {
            BigDecimal paid = totalPaidMap.getOrDefault(p.getId(), BigDecimal.ZERO).setScale(SCALE, ROUNDING_MODE);
            BigDecimal share = totalShareMap.getOrDefault(p.getId(), BigDecimal.ZERO).setScale(SCALE, ROUNDING_MODE);
            BigDecimal net = paid.subtract(share).setScale(SCALE, ROUNDING_MODE);
            balances.add(new ParticipantBalanceDto(p.getId(), p.getName(), paid, share, net));
        }

        return balances;
    }

    public List<SettlementTransactionDto> calculateSettlements(List<ParticipantBalanceDto> balances) {
        List<BalanceEntry> creditors = new ArrayList<>();
        List<BalanceEntry> debtors = new ArrayList<>();

        for (ParticipantBalanceDto balance : balances) {
            BigDecimal net = balance.netBalance();
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new BalanceEntry(balance.participantId(), balance.participantName(), net));
            } else if (net.compareTo(BigDecimal.ZERO) < 0) {
                // Debtor owes absolute value
                debtors.add(new BalanceEntry(balance.participantId(), balance.participantName(), net.abs()));
            }
        }

        // Sort descending by amount to optimize matches
        creditors.sort(Comparator.comparing(BalanceEntry::getAmount).reversed());
        debtors.sort(Comparator.comparing(BalanceEntry::getAmount).reversed());

        List<SettlementTransactionDto> settlements = new ArrayList<>();

        int debtorIdx = 0;
        int creditorIdx = 0;

        while (debtorIdx < debtors.size() && creditorIdx < creditors.size()) {
            BalanceEntry debtor = debtors.get(debtorIdx);
            BalanceEntry creditor = creditors.get(creditorIdx);

            BigDecimal debtAmount = debtor.getAmount();
            BigDecimal creditAmount = creditor.getAmount();

            BigDecimal transferAmount = debtAmount.min(creditAmount).setScale(SCALE, ROUNDING_MODE);

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                settlements.add(new SettlementTransactionDto(
                    debtor.getId(),
                    debtor.getName(),
                    creditor.getId(),
                    creditor.getName(),
                    transferAmount
                ));
            }

            debtor.setAmount(debtAmount.subtract(transferAmount));
            creditor.setAmount(creditAmount.subtract(transferAmount));

            if (debtor.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                debtorIdx++;
            }
            if (creditor.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                creditorIdx++;
            }
        }

        return settlements;
    }

    private static class BalanceEntry {
        private final UUID id;
        private final String name;
        private BigDecimal amount;

        public BalanceEntry(UUID id, String name, BigDecimal amount) {
            this.id = id;
            this.name = name;
            this.amount = amount;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
