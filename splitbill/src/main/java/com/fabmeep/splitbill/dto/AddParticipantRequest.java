package com.fabmeep.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddParticipantRequest(
    @NotBlank(message = "Participant name cannot be blank")
    @Size(min = 1, max = 100, message = "Participant name must be between 1 and 100 characters")
    String name
) {}
