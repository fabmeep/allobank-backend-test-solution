package com.fabmeep.splitbill.dto;

import com.fabmeep.splitbill.entity.Participant;

import java.util.UUID;

public record ParticipantResponse(
    UUID id,
    String name
) {
    public static ParticipantResponse fromEntity(Participant participant) {
        return new ParticipantResponse(participant.getId(), participant.getName());
    }
}
