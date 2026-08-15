package com.fabmeep.splitbill.repository;

import com.fabmeep.splitbill.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("SELECT e FROM Expense e " +
           "LEFT JOIN FETCH e.payer " +
           "LEFT JOIN FETCH e.splits s " +
           "LEFT JOIN FETCH s.participant " +
           "WHERE e.group.id = :groupId ORDER BY e.createdAt ASC")
    List<Expense> findByGroupIdWithDetails(@Param("groupId") UUID groupId);

    @Query("SELECT e FROM Expense e " +
           "LEFT JOIN FETCH e.payer " +
           "LEFT JOIN FETCH e.splits s " +
           "LEFT JOIN FETCH s.participant " +
           "WHERE e.id = :id AND e.group.id = :groupId")
    Optional<Expense> findByIdAndGroupIdWithDetails(@Param("id") UUID id, @Param("groupId") UUID groupId);
}
