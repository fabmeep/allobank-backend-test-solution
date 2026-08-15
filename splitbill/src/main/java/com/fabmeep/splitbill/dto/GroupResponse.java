package com.fabmeep.splitbill.dto;

import com.fabmeep.splitbill.entity.BillGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(
    UUID id,
    String name,
    String description,
    List<ParticipantResponse> participants,
    Instant createdAt
) {
    public static GroupResponse fromEntity(BillGroup group) {
        List<ParticipantResponse> participantResponses = group.getParticipants() != null
            ? group.getParticipants().stream().map(ParticipantResponse::fromEntity).toList()
            : List.of();
        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getDescription(),
            participantResponses,
            group.getCreatedAt()
        );
    }
}
