package com.fabmeep.splitbill.repository;

import com.fabmeep.splitbill.entity.BillGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillGroupRepository extends JpaRepository<BillGroup, UUID> {

    @Query("SELECT g FROM BillGroup g LEFT JOIN FETCH g.participants WHERE g.id = :id")
    Optional<BillGroup> findByIdWithParticipants(@Param("id") UUID id);
}
