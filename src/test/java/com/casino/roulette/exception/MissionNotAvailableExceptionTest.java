package com.casino.roulette.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MissionNotAvailableExceptionTest {

    @Test
    void testDefaultConstructor() {
        MissionNotAvailableException exception = new MissionNotAvailableException();
        
        assertEquals(MissionNotAvailableException.ERROR_CODE, exception.getErrorCode());
        assertEquals("Mission is not available for claiming", exception.getMessage());
    }

    @Test
    void testConstructorWithMessage() {
        String customMessage = "Custom mission not available message";
        
        MissionNotAvailableException exception = new MissionNotAvailableException(customMessage);
        
        assertEquals(MissionNotAvailableException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String customMessage = "Custom mission not available message";
        Throwable cause = new RuntimeException("Root cause");
        
        MissionNotAvailableException exception = new MissionNotAvailableException(customMessage, cause);
        
        assertEquals(MissionNotAvailableException.ERROR_CODE, exception.getErrorCode());
        assertEquals(customMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testErrorCodeConstant() {
        assertEquals("MISSION_NOT_AVAILABLE", MissionNotAvailableException.ERROR_CODE);
    }

    @Test
    void testIsBusinessException() {
        MissionNotAvailableException exception = new MissionNotAvailableException();
        
        assertTrue(exception instanceof BusinessException);
    }
}