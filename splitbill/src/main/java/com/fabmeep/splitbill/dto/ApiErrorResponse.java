package com.fabmeep.splitbill.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    boolean success,
    String message,
    int status,
    List<String> errors,
    Instant timestamp
) {
    public static ApiErrorResponse of(int status, String message) {
        return new ApiErrorResponse(false, message, status, null, Instant.now());
    }

    public static ApiErrorResponse of(int status, String message, List<String> errors) {
        return new ApiErrorResponse(false, message, status, errors, Instant.now());
    }
}
