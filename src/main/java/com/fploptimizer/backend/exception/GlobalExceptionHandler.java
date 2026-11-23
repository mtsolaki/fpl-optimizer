package com.fploptimizer.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

/**
 * Centralized exception handling interceptor for REST controllers.
 * Translates application exceptions into standardized error payloads.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String NOT_FOUND_REASON = "Not Found";
    private static final String BAD_REQUEST_REASON = "Bad Request";
    private static final String INTERNAL_SERVER_ERROR_REASON = "Internal Server Error";

    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message
    ) {}

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, NOT_FOUND_REASON, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, BAD_REQUEST_REASON, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server error: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_REASON,
                "An unexpected error occurred. Please check server logs."
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus httpStatus, String error, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(ZoneOffset.UTC),
                httpStatus.value(),
                error,
                message
        );
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
}

