package com.fabmeep.splitbill.exception;

import com.fabmeep.splitbill.dto.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Not found");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should handle BadRequestException")
    void testHandleBadRequest() {
        BadRequestException ex = new BadRequestException("Bad request");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Bad request");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleInvalidJson(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Malformed request body");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void testHandleGenericException() {
        Exception ex = new RuntimeException("Unexpected crash");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An unexpected internal server error occurred");
        assertThat(response.getBody().status()).isEqualTo(500);
    }
}
