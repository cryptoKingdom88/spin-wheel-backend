package com.casino.roulette.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsufficientLettersExceptionTest {

    @Test
    void testDefaultConstructor() {
        InsufficientLettersException exception = new InsufficientLettersException();
        
        assertEquals(InsufficientLettersException.ERROR_CODE, exception.getErrorCode());
        assertEquals("You don't have enough letters to claim this word bonus", exception.getMessage());
    }

    @Test
    void testConstructorWithMessage() {
        String customMessage = "Custom insufficient letters message";
        
        InsufficientLettersException exception = new InsufficientLettersException(customMessage);
        
        assertEquals(InsufficientLettersException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String customMessage = "Custom insufficient letters message";
        Throwable cause = new RuntimeException("Root cause");
        
        InsufficientLettersException exception = new InsufficientLettersException(customMessage, cause);
        
        assertEquals(InsufficientLettersException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testErrorCodeConstant() {
        assertEquals("INSUFFICIENT_LETTERS", InsufficientLettersException.ERROR_CODE);
    }

    @Test
    void testIsBusinessException() {
        InsufficientLettersException exception = new InsufficientLettersException();
        
        assertTrue(exception instanceof BusinessException);
    }
}