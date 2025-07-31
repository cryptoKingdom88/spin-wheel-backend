package com.casino.roulette.exception;

/**
 * Exception thrown when a deposit amount is invalid (negative, zero, or exceeds limits).
 */
public class InvalidDepositAmountException extends BusinessException {
    
    public static final String ERROR_CODE = "INVALID_DEPOSIT_AMOUNT";
    
    public InvalidDepositAmountException() {
        super(ERROR_CODE, "Invalid deposit amount");
    }
    
    public InvalidDepositAmountException(String message) {
        super(ERROR_CODE, message);
    }
    
    public InvalidDepositAmountException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}