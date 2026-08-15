package com.fabmeep.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
    @NotBlank(message = "Group name is required")
    @Size(min = 2, max = 100, message = "Group name must be between 2 and 100 characters")
    String name,

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,

    @NotEmpty(message = "At least one participant is required")
    @Size(min = 2, message = "At least 2 participants are required to split bills")
    List<@NotBlank(message = "Participant name cannot be blank") String> participants
) {}
