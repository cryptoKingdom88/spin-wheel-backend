package com.casino.roulette.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidDepositAmountExceptionTest {

    @Test
    void testDefaultConstructor() {
        InvalidDepositAmountException exception = new InvalidDepositAmountException();
        
        assertEquals(InvalidDepositAmountException.ERROR_CODE, exception.getErrorCode());
        assertEquals("Invalid deposit amount", exception.getMessage());
    }

    @Test
    void testConstructorWithMessage() {
        String customMessage = "Custom invalid deposit amount message";
        
        InvalidDepositAmountException exception = new InvalidDepositAmountException(customMessage);
        
        assertEquals(InvalidDepositAmountException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String customMessage = "Custom invalid deposit amount message";
        Throwable cause = new RuntimeException("Root cause");
        
        InvalidDepositAmountException exception = new InvalidDepositAmountException(customMessage, cause);
        
        assertEquals(InvalidDepositAmountException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testErrorCodeConstant() {
        assertEquals("INVALID_DEPOSIT_AMOUNT", InvalidDepositAmountException.ERROR_CODE);
    }

    @Test
    void testIsBusinessException() {
        InvalidDepositAmountException exception = new InvalidDepositAmountException();
        
        assertTrue(exception instanceof BusinessException);
    }
}