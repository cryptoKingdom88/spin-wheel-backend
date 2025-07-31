package com.casino.roulette.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleBusinessException() {
        InsufficientSpinsException exception = new InsufficientSpinsException();
        
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("INSUFFICIENT_SPINS", body.get("error"));
        assertEquals("You don't have enough spins available", body.get("message"));
        assertNotNull(body.get("timestamp"));
        assertTrue(body.get("timestamp") instanceof LocalDateTime);
    }

    @Test
    void testHandleInsufficientLettersException() {
        InsufficientLettersException exception = new InsufficientLettersException();
        
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("INSUFFICIENT_LETTERS", body.get("error"));
        assertEquals("You don't have enough letters to claim this word bonus", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleMissionNotAvailableException() {
        MissionNotAvailableException exception = new MissionNotAvailableException();
        
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("MISSION_NOT_AVAILABLE", body.get("error"));
        assertEquals("Mission is not available for claiming", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleInvalidDepositAmountException() {
        InvalidDepositAmountException exception = new InvalidDepositAmountException();
        
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("INVALID_DEPOSIT_AMOUNT", body.get("error"));
        assertEquals("Invalid deposit amount", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleCustomBusinessException() {
        BusinessException exception = new BusinessException("CUSTOM_ERROR", "Custom error message");
        
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("CUSTOM_ERROR", body.get("error"));
        assertEquals("Custom error message", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleMissingRequestHeaderException() {
        // Create a mock exception with a custom message since the constructor is complex
        MissingRequestHeaderException exception = new MissingRequestHeaderException("Required-Header", null) {
            @Override
            public String getMessage() {
                return "Required request header 'Required-Header' is not present";
            }
        };
        
        ResponseEntity<Map<String, Object>> response = handler.handleMissingRequestHeaderException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("MISSING_HEADER", body.get("error"));
        assertEquals("Required request header 'Required-Header' is not present", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");
        
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(exception);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("INVALID_REQUEST", body.get("error"));
        assertEquals("Invalid argument provided", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleIllegalStateException() {
        IllegalStateException exception = new IllegalStateException("Operation not allowed");
        
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalStateException(exception);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("OPERATION_NOT_ALLOWED", body.get("error"));
        assertEquals("Operation not allowed", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void testHandleGenericException() {
        RuntimeException exception = new RuntimeException("Unexpected error");
        
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(exception);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("INTERNAL_ERROR", body.get("error"));
        assertEquals("An unexpected error occurred", body.get("message"));
        assertNotNull(body.get("timestamp"));
    }
}