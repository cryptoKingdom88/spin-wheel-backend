package com.casino.roulette.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void testBusinessExceptionWithCodeAndMessage() {
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        
        BusinessException exception = new BusinessException(errorCode, message);
        
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testBusinessExceptionWithCodeMessageAndCause() {
        String errorCode = "TEST_ERROR";
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        
        BusinessException exception = new BusinessException(errorCode, message, cause);
        
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testBusinessExceptionIsRuntimeException() {
        BusinessException exception = new BusinessException("CODE", "Message");
        
        assertTrue(exception instanceof RuntimeException);
    }
}