package com.casino.roulette.exception;

/**
 * Base exception class for business logic errors in the roulette spin system.
 * All business-related exceptions should extend this class.
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}