package com.fabmeep.splitbill.repository;

import com.fabmeep.splitbill.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByGroupId(UUID groupId);

    Optional<Participant> findByIdAndGroupId(UUID id, UUID groupId);

    boolean existsByGroupIdAndNameIgnoreCase(UUID groupId, String name);
}
