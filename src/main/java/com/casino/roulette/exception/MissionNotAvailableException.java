package com.casino.roulette.exception;

/**
 * Exception thrown when a user attempts to claim a mission that is not available or eligible.
 */
public class MissionNotAvailableException extends BusinessException {
    
    public static final String ERROR_CODE = "MISSION_NOT_AVAILABLE";
    
    public MissionNotAvailableException() {
        super(ERROR_CODE, "Mission is not available for claiming");
    }
    
    public MissionNotAvailableException(String message) {
        super(ERROR_CODE, message);
    }
    
    public MissionNotAvailableException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}