package com.casino.roulette.exception;

/**
 * Exception thrown when a user is not found in the system
 */
public class UserNotFoundException extends RuntimeException {
    
    private final Long userId;
    
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
        this.userId = userId;
    }
    
    public UserNotFoundException(Long userId, String message) {
        super(message);
        this.userId = userId;
    }
    
    public Long getUserId() {
        return userId;
    }
}