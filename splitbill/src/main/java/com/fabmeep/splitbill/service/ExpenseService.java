package com.fabmeep.splitbill.service;

import com.fabmeep.splitbill.dto.AddExpenseRequest;
import com.fabmeep.splitbill.dto.ExpenseResponse;
import com.fabmeep.splitbill.dto.ExpenseSplitItemRequest;
import com.fabmeep.splitbill.entity.BillGroup;
import com.fabmeep.splitbill.entity.Expense;
import com.fabmeep.splitbill.entity.ExpenseSplit;
import com.fabmeep.splitbill.entity.Participant;
import com.fabmeep.splitbill.enums.SplitType;
import com.fabmeep.splitbill.exception.BadRequestException;
import com.fabmeep.splitbill.exception.ResourceNotFoundException;
import com.fabmeep.splitbill.repository.BillGroupRepository;
import com.fabmeep.splitbill.repository.ExpenseRepository;
import com.fabmeep.splitbill.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BillGroupRepository groupRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;

    public ExpenseService(
        BillGroupRepository groupRepository,
        ParticipantRepository participantRepository,
        ExpenseRepository expenseRepository
    ) {
        this.groupRepository = groupRepository;
        this.participantRepository = participantRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public ExpenseResponse addExpense(UUID groupId, AddExpenseRequest request) {
        BillGroup group = groupRepository.findByIdWithParticipants(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Bill group not found with id: " + groupId));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Expense amount must be greater than zero");
        }

        BigDecimal totalAmount = request.amount().setScale(SCALE, ROUNDING_MODE);

        // Fetch participant map for the group
        Map<UUID, Participant> participantMap = group.getParticipants().stream()
            .collect(Collectors.toMap(Participant::getId, Function.identity()));

        Participant payer = participantMap.get(request.payerId());
        if (payer == null) {
            throw new BadRequestException("Payer does not belong to this bill group");
        }

        SplitType splitType = request.splitType() != null ? request.splitType() : SplitType.EQUAL;
        Expense expense = new Expense(request.description().trim(), totalAmount, payer, group, splitType);

        List<ExpenseSplit> splits = calculateSplits(expense, totalAmount, splitType, request, participantMap);
        for (ExpenseSplit split : splits) {
            expense.addSplit(split);
        }

        Expense savedExpense = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(savedExpense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByGroup(UUID groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Bill group not found with id: " + groupId);
        }
        return expenseRepository.findByGroupIdWithDetails(groupId).stream()
            .map(ExpenseResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID groupId, UUID expenseId) {
        Expense expense = expenseRepository.findByIdAndGroupIdWithDetails(expenseId, groupId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Expense not found with id: " + expenseId + " for group: " + groupId
            ));
        return ExpenseResponse.fromEntity(expense);
    }

    private List<ExpenseSplit> calculateSplits(
        Expense expense,
        BigDecimal totalAmount,
        SplitType splitType,
        AddExpenseRequest request,
        Map<UUID, Participant> participantMap
    ) {
        return switch (splitType) {
            case EQUAL -> calculateEqualSplits(expense, totalAmount, request.participantIds(), participantMap);
            case EXACT -> calculateExactSplits(expense, totalAmount, request.splits(), participantMap);
            case PERCENTAGE -> calculatePercentageSplits(expense, totalAmount, request.splits(), participantMap);
        };
    }

    private List<ExpenseSplit> calculateEqualSplits(
        Expense expense,
        BigDecimal totalAmount,
        List<UUID> participantIds,
        Map<UUID, Participant> participantMap
    ) {
        List<Participant> targetParticipants;
        if (participantIds == null || participantIds.isEmpty()) {
            targetParticipants = new ArrayList<>(participantMap.values());
        } else {
            Set<UUID> uniqueIds = new HashSet<>(participantIds);
            targetParticipants = new ArrayList<>();
            for (UUID id : uniqueIds) {
                Participant p = participantMap.get(id);
                if (p == null) {
                    throw new BadRequestException("Participant with id " + id + " does not belong to this bill group");
                }
                targetParticipants.add(p);
            }
        }

        if (targetParticipants.isEmpty()) {
            throw new BadRequestException("No participants specified for equal split");
        }

        int count = targetParticipants.size();
        BigDecimal countBd = BigDecimal.valueOf(count);
        BigDecimal baseShare = totalAmount.divide(countBd, SCALE, RoundingMode.DOWN);
        BigDecimal totalAllocated = baseShare.multiply(countBd);
        BigDecimal remainder = totalAmount.subtract(totalAllocated);

        // Remainder cents to distribute
        int remainderCents = remainder.movePointRight(2).intValueExact();

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Participant p = targetParticipants.get(i);
            BigDecimal share = baseShare;
            if (i < remainderCents) {
                share = share.add(new BigDecimal("0.01"));
            }
            splits.add(new ExpenseSplit(expense, p, share));
        }

        return splits;
    }

    private List<ExpenseSplit> calculateExactSplits(
        Expense expense,
        BigDecimal totalAmount,
        List<ExpenseSplitItemRequest> splitItems,
        Map<UUID, Participant> participantMap
    ) {
        if (splitItems == null || splitItems.isEmpty()) {
            throw new BadRequestException("Exact split items are required for EXACT split type");
        }

        Set<UUID> seenParticipants = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        List<ExpenseSplit> splits = new ArrayList<>();

        for (ExpenseSplitItemRequest item : splitItems) {
            if (item.participantId() == null) {
                throw new BadRequestException("Participant ID cannot be null in split item");
            }
            if (item.amount() == null || item.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Split amount must be greater than zero");
            }
            if (!seenParticipants.add(item.participantId())) {
                throw new BadRequestException("Duplicate participant in split items: " + item.participantId());
            }

            Participant participant = participantMap.get(item.participantId());
            if (participant == null) {
                throw new BadRequestException("Participant " + item.participantId() + " does not belong to this group");
            }

            BigDecimal itemAmount = item.amount().setScale(SCALE, ROUNDING_MODE);
            sum = sum.add(itemAmount);
            splits.add(new ExpenseSplit(expense, participant, itemAmount));
        }

        if (sum.compareTo(totalAmount) != 0) {
            throw new BadRequestException(
                "The sum of split amounts (" + sum + ") must equal the total expense amount (" + totalAmount + ")"
            );
        }

        return splits;
    }

    private List<ExpenseSplit> calculatePercentageSplits(
        Expense expense,
        BigDecimal totalAmount,
        List<ExpenseSplitItemRequest> splitItems,
        Map<UUID, Participant> participantMap
    ) {
        if (splitItems == null || splitItems.isEmpty()) {
            throw new BadRequestException("Split items with percentages are required for PERCENTAGE split type");
        }

        Set<UUID> seenParticipants = new HashSet<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (ExpenseSplitItemRequest item : splitItems) {
            if (item.participantId() == null) {
                throw new BadRequestException("Participant ID cannot be null in split item");
            }
            if (item.percentage() == null || item.percentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Percentage must be greater than zero");
            }
            if (!seenParticipants.add(item.participantId())) {
                throw new BadRequestException("Duplicate participant in split items: " + item.participantId());
            }
            if (!participantMap.containsKey(item.participantId())) {
                throw new BadRequestException("Participant " + item.participantId() + " does not belong to this group");
            }
            totalPercentage = totalPercentage.add(item.percentage());
        }

        if (totalPercentage.compareTo(new BigDecimal("100.00")) != 0 && totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new BadRequestException("Split percentages must sum to 100%, currently sums to " + totalPercentage + "%");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal sumCalculated = BigDecimal.ZERO;

        for (ExpenseSplitItemRequest item : splitItems) {
            Participant participant = participantMap.get(item.participantId());
            BigDecimal amount = totalAmount.multiply(item.percentage())
                .divide(new BigDecimal("100"), SCALE, RoundingMode.DOWN);
            sumCalculated = sumCalculated.add(amount);
            splits.add(new ExpenseSplit(expense, participant, amount, item.percentage().setScale(2, ROUNDING_MODE)));
        }

        // Distribute any remainder cents to the first participant(s)
        BigDecimal diff = totalAmount.subtract(sumCalculated);
        int remainderCents = diff.movePointRight(2).intValueExact();
        for (int i = 0; i < remainderCents; i++) {
            ExpenseSplit s = splits.get(i);
            s.setAmount(s.getAmount().add(new BigDecimal("0.01")));
        }

        return splits;
    }
}
