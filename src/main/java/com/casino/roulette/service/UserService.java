package com.casino.roulette.service;

import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final TransactionLogRepository transactionLogRepository;
    
    @Autowired
    public UserService(UserRepository userRepository, TransactionLogRepository transactionLogRepository) {
        this.userRepository = userRepository;
        this.transactionLogRepository = transactionLogRepository;
    }
    
    /**
     * Get user by ID, creating if not exists
     */
    public User getOrCreateUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return userRepository.findById(userId)
                .orElseGet(() -> {
                    User newUser = new User(userId);
                    return userRepository.save(newUser);
                });
    }
    
    /**
     * Update user's cash balance and log the transaction
     */
    public void updateCashBalance(Long userId, BigDecimal amount) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        // Ensure user exists
        User user = getOrCreateUser(userId);
        
        // Check if balance would go negative
        BigDecimal newBalance = user.getCashBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Current: " + user.getCashBalance() + ", Requested: " + amount);
        }
        
        // Update balance
        int updatedRows = userRepository.updateCashBalance(userId, amount);
        if (updatedRows == 0) {
            throw new RuntimeException("Failed to update cash balance for user: " + userId);
        }
        
        // Log the transaction
        String description = amount.compareTo(BigDecimal.ZERO) > 0 
            ? "Cash balance increased by $" + amount
            : "Cash balance decreased by $" + amount.abs();
            
        TransactionLog log = new TransactionLog(userId, "BALANCE_UPDATE", amount, description);
        transactionLogRepository.save(log);
    }
    
    /**
     * Grant daily login spin if eligible
     * Returns true if spin was granted, false if already granted today
     */
    public boolean grantDailyLoginSpin(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = getOrCreateUser(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        // Check if user already logged in today
        if (user.getLastDailyLogin() != null) {
            LocalDate lastLoginDate = user.getLastDailyLogin().toLocalDate();
            if (lastLoginDate.equals(today)) {
                return false; // Already granted today
            }
        }
        
        // Grant daily login spin
        int updatedSpins = userRepository.updateAvailableSpins(userId, 1);
        if (updatedSpins == 0) {
            throw new RuntimeException("Failed to grant daily login spin for user: " + userId);
        }
        
        // Update last daily login timestamp
        int updatedLogin = userRepository.updateLastDailyLogin(userId, now);
        if (updatedLogin == 0) {
            throw new RuntimeException("Failed to update last daily login for user: " + userId);
        }
        
        // Log the transaction
        TransactionLog log = TransactionLog.createDailyLoginSpinLog(userId);
        transactionLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Process first deposit bonus if eligible
     */
    public void processFirstDepositBonus(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        User user = getOrCreateUser(userId);
        
        // Check if first deposit bonus already used
        if (user.getFirstDepositBonusUsed()) {
            return; // Already used, no action needed
        }
        
        // Grant first deposit bonus spin
        int updatedSpins = userRepository.updateAvailableSpins(userId, 1);
        if (updatedSpins == 0) {
            throw new RuntimeException("Failed to grant first deposit bonus spin for user: " + userId);
        }
        
        // Mark first deposit bonus as used
        int updatedBonus = userRepository.markFirstDepositBonusUsed(userId);
        if (updatedBonus == 0) {
            throw new RuntimeException("Failed to mark first deposit bonus as used for user: " + userId);
        }
        
        // Log the transaction
        TransactionLog log = TransactionLog.createFirstDepositSpinLog(userId);
        transactionLogRepository.save(log);
    }
    
    /**
     * Get user by ID (read-only operation)
     */
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return userRepository.findById(userId).orElse(null);
    }
    
    /**
     * Check if user has sufficient spins
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientSpins(Long userId, Integer requiredSpins) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (requiredSpins == null || requiredSpins < 0) {
            throw new IllegalArgumentException("Required spins must be non-negative");
        }
        
        return userRepository.hasSufficientSpins(userId, requiredSpins);
    }
    
    /**
     * Consume spins from user's balance
     */
    public boolean consumeSpins(Long userId, Integer spins) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (spins == null || spins <= 0) {
            throw new IllegalArgumentException("Spins must be positive");
        }
        
        // Consume spins
        int updatedRows = userRepository.consumeSpins(userId, spins);
        if (updatedRows == 0) {
            return false; // Insufficient spins
        }
        
        // Log the transaction only if successful
        TransactionLog log = TransactionLog.createSpinConsumedLog(userId);
        transactionLogRepository.save(log);
        
        return true;
    }
    
    /**
     * Grant spins to user
     */
    public void grantSpins(Long userId, Integer spins, String reason) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (spins == null || spins <= 0) {
            throw new IllegalArgumentException("Spins must be positive");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        
        // Ensure user exists
        getOrCreateUser(userId);
        
        // Grant spins
        int updatedRows = userRepository.updateAvailableSpins(userId, spins);
        if (updatedRows == 0) {
            throw new RuntimeException("Failed to grant spins for user: " + userId);
        }
        
        // Log the transaction
        TransactionLog log = new TransactionLog(userId, "SPINS_GRANTED", 
            spins + " spin(s) granted: " + reason);
        transactionLogRepository.save(log);
    }
}