package com.casino.roulette.service;

import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TransactionService {
    
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    
    @Autowired
    public TransactionService(TransactionLogRepository transactionLogRepository,
                             UserService userService) {
        this.transactionLogRepository = transactionLogRepository;
        this.userService = userService;
    }
    
    /**
     * Log a transaction for a user
     */
    public TransactionLog logTransaction(Long userId, String transactionType, String description) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        
        // Ensure user exists
        userService.getOrCreateUser(userId);
        
        TransactionLog log = new TransactionLog(userId, transactionType, description);
        return transactionLogRepository.save(log);
    }
    
    /**
     * Log a transaction with amount for a user
     */
    public TransactionLog logTransaction(Long userId, String transactionType, BigDecimal amount, String description) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }
        
        // Ensure user exists
        userService.getOrCreateUser(userId);
        
        TransactionLog log = new TransactionLog(userId, transactionType, amount, description);
        return transactionLogRepository.save(log);
    }
    
    /**
     * Get transaction history for a user (paginated)
     */
    @Transactional(readOnly = true)
    public Page<TransactionLog> getTransactionHistory(Long userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionLogRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Get transaction history for a user (all transactions)
     */
    @Transactional(readOnly = true)
    public List<TransactionLog> getTransactionHistory(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return transactionLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get transaction history for a user by type
     */
    @Transactional(readOnly = true)
    public List<TransactionLog> getTransactionHistoryByType(Long userId, String transactionType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type cannot be null or empty");
        }
        
        return transactionLogRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(userId, transactionType);
    }
    
    /**
     * Get transaction history for a user within date range
     */
    @Transactional(readOnly = true)
    public List<TransactionLog> getTransactionHistoryByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        return transactionLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDate, endDate);
    }
    
    /**
     * Get recent transactions for a user (last N transactions)
     */
    @Transactional(readOnly = true)
    public List<TransactionLog> getRecentTransactions(Long userId, int limit) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionLogRepository.findByUserId(userId, pageable).getContent();
    }
    
    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public Optional<TransactionLog> getTransactionById(Long transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        
        return transactionLogRepository.findById(transactionId);
    }
    
    /**
     * Get total transaction count for a user
     */
    @Transactional(readOnly = true)
    public long getTransactionCount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return transactionLogRepository.countByUserId(userId);
    }
    
    /**
     * Get transaction count for a user by type
     */
    @Transactional(readOnly = true)
    public long getTransactionCountByType(Long userId, String transactionType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type cannot be null or empty");
        }
        
        return transactionLogRepository.countByUserIdAndTransactionType(userId, transactionType);
    }
    
    /**
     * Get total cash amount from transactions for a user
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCashAmount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        BigDecimal total = transactionLogRepository.sumAmountByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Get total cash amount from transactions for a user by type
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCashAmountByType(Long userId, String transactionType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (transactionType == null || transactionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type cannot be null or empty");
        }
        
        BigDecimal total = transactionLogRepository.sumAmountByUserIdAndTransactionType(userId, transactionType);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Get cash transactions (positive amounts) for a user
     */
    @Transactional(readOnly = true)
    public List<TransactionLog> getCashCreditTransactions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return transactionLogRepository.findByUserIdAndAmountGreaterThanOrderByCreatedAtDesc(userId, BigDecimal.ZERO);
    }
    
    /**
     * Get cash debit transactions (negative amounts) for a user
     */
    @Transactional(readOnly = true)
    public List<TransactionLog> getCashDebitTransactions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return transactionLogRepository.findByUserIdAndAmountLessThanOrderByCreatedAtDesc(userId, BigDecimal.ZERO);
    }
    
    /**
     * Check if user has any transactions
     */
    @Transactional(readOnly = true)
    public boolean hasTransactions(Long userId) {
        if (userId == null) {
            return false;
        }
        
        return transactionLogRepository.existsByUserId(userId);
    }
    
    /**
     * Check if user has transactions of a specific type
     */
    @Transactional(readOnly = true)
    public boolean hasTransactionsOfType(Long userId, String transactionType) {
        if (userId == null || transactionType == null || transactionType.trim().isEmpty()) {
            return false;
        }
        
        return transactionLogRepository.existsByUserIdAndTransactionType(userId, transactionType);
    }
    
    /**
     * Get transaction statistics for a user
     */
    @Transactional(readOnly = true)
    public TransactionStatistics getTransactionStatistics(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        long totalCount = getTransactionCount(userId);
        BigDecimal totalAmount = getTotalCashAmount(userId);
        
        long depositCount = getTransactionCountByType(userId, TransactionLog.TYPE_DEPOSIT);
        BigDecimal totalDeposits = getTotalCashAmountByType(userId, TransactionLog.TYPE_DEPOSIT);
        
        long rouletteWinCount = getTransactionCountByType(userId, TransactionLog.TYPE_ROULETTE_WIN);
        BigDecimal totalRouletteWins = getTotalCashAmountByType(userId, TransactionLog.TYPE_ROULETTE_WIN);
        
        long letterBonusCount = getTransactionCountByType(userId, TransactionLog.TYPE_LETTER_BONUS);
        BigDecimal totalLetterBonuses = getTotalCashAmountByType(userId, TransactionLog.TYPE_LETTER_BONUS);
        
        long spinCount = getTransactionCountByType(userId, TransactionLog.TYPE_SPIN_CONSUMED);
        
        return new TransactionStatistics(
            totalCount, totalAmount,
            depositCount, totalDeposits,
            rouletteWinCount, totalRouletteWins,
            letterBonusCount, totalLetterBonuses,
            spinCount
        );
    }
    
    /**
     * Inner class for transaction statistics
     */
    public static class TransactionStatistics {
        private final long totalTransactions;
        private final BigDecimal totalAmount;
        private final long depositCount;
        private final BigDecimal totalDeposits;
        private final long rouletteWinCount;
        private final BigDecimal totalRouletteWins;
        private final long letterBonusCount;
        private final BigDecimal totalLetterBonuses;
        private final long totalSpins;
        
        public TransactionStatistics(long totalTransactions, BigDecimal totalAmount,
                                   long depositCount, BigDecimal totalDeposits,
                                   long rouletteWinCount, BigDecimal totalRouletteWins,
                                   long letterBonusCount, BigDecimal totalLetterBonuses,
                                   long totalSpins) {
            this.totalTransactions = totalTransactions;
            this.totalAmount = totalAmount;
            this.depositCount = depositCount;
            this.totalDeposits = totalDeposits;
            this.rouletteWinCount = rouletteWinCount;
            this.totalRouletteWins = totalRouletteWins;
            this.letterBonusCount = letterBonusCount;
            this.totalLetterBonuses = totalLetterBonuses;
            this.totalSpins = totalSpins;
        }
        
        // Getters
        public long getTotalTransactions() { return totalTransactions; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public long getDepositCount() { return depositCount; }
        public BigDecimal getTotalDeposits() { return totalDeposits; }
        public long getRouletteWinCount() { return rouletteWinCount; }
        public BigDecimal getTotalRouletteWins() { return totalRouletteWins; }
        public long getLetterBonusCount() { return letterBonusCount; }
        public BigDecimal getTotalLetterBonuses() { return totalLetterBonuses; }
        public long getTotalSpins() { return totalSpins; }
        
        @Override
        public String toString() {
            return "TransactionStatistics{" +
                    "totalTransactions=" + totalTransactions +
                    ", totalAmount=" + totalAmount +
                    ", depositCount=" + depositCount +
                    ", totalDeposits=" + totalDeposits +
                    ", rouletteWinCount=" + rouletteWinCount +
                    ", totalRouletteWins=" + totalRouletteWins +
                    ", letterBonusCount=" + letterBonusCount +
                    ", totalLetterBonuses=" + totalLetterBonuses +
                    ", totalSpins=" + totalSpins +
                    '}';
        }
    }
}