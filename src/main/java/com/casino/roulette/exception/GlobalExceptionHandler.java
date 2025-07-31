package com.casino.roulette.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global exception handler for REST API controllers
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        HttpStatus status = determineHttpStatus(e);
        
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", e.getErrorCode(),
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Determine appropriate HTTP status for business exceptions
     */
    private HttpStatus determineHttpStatus(BusinessException e) {
        return switch (e.getErrorCode()) {
            case "INSUFFICIENT_SPINS", "INSUFFICIENT_LETTERS" -> HttpStatus.CONFLICT;
            case "MISSION_NOT_AVAILABLE" -> HttpStatus.FORBIDDEN;
            case "INVALID_DEPOSIT_AMOUNT" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
    
    /**
     * Handle validation constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException e) {
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", "VALIDATION_ERROR",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle method argument validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
            
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", "VALIDATION_ERROR",
            "message", message,
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle missing request headers
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", "MISSING_HEADER",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", "INVALID_REQUEST",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException e) {
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", "OPERATION_NOT_ALLOWED",
            "message", e.getMessage(),
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.unprocessableEntity().body(errorResponse);
    }
    
    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        Map<String, Object> errorResponse = Map.of(
            "success", false,
            "error", "INTERNAL_ERROR",
            "message", "An unexpected error occurred",
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}