package com.casino.roulette.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsufficientSpinsExceptionTest {

    @Test
    void testDefaultConstructor() {
        InsufficientSpinsException exception = new InsufficientSpinsException();
        
        assertEquals(InsufficientSpinsException.ERROR_CODE, exception.getErrorCode());
        assertEquals("You don't have enough spins available", exception.getMessage());
    }

    @Test
    void testConstructorWithMessage() {
        String customMessage = "Custom insufficient spins message";
        
        InsufficientSpinsException exception = new InsufficientSpinsException(customMessage);
        
        assertEquals(InsufficientSpinsException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String customMessage = "Custom insufficient spins message";
        Throwable cause = new RuntimeException("Root cause");
        
        InsufficientSpinsException exception = new InsufficientSpinsException(customMessage, cause);
        
        assertEquals(InsufficientSpinsException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testErrorCodeConstant() {
        assertEquals("INSUFFICIENT_SPINS", InsufficientSpinsException.ERROR_CODE);
    }

    @Test
    void testIsBusinessException() {
        InsufficientSpinsException exception = new InsufficientSpinsException();
        
        assertTrue(exception instanceof BusinessException);
    }
}