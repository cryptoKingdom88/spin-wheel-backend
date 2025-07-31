package com.casino.roulette.exception;

/**
 * Exception thrown when a user attempts to claim a word bonus but lacks the required letters.
 */
public class InsufficientLettersException extends BusinessException {
    
    public static final String ERROR_CODE = "INSUFFICIENT_LETTERS";
    
    public InsufficientLettersException() {
        super(ERROR_CODE, "You don't have enough letters to claim this word bonus");
    }
    
    public InsufficientLettersException(String message) {
        super(ERROR_CODE, message);
    }
    
    public InsufficientLettersException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}