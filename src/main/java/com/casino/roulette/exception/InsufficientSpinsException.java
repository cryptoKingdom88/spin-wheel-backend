package com.casino.roulette.exception;

/**
 * Exception thrown when a user attempts to spin the roulette but has no available spins.
 */
public class InsufficientSpinsException extends BusinessException {
    
    public static final String ERROR_CODE = "INSUFFICIENT_SPINS";
    
    public InsufficientSpinsException() {
        super(ERROR_CODE, "You don't have enough spins available");
    }
    
    public InsufficientSpinsException(String message) {
        super(ERROR_CODE, message);
    }
    
    public InsufficientSpinsException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}