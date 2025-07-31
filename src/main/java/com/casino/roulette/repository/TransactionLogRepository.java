package com.casino.roulette.repository;

import com.casino.roulette.entity.TransactionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    
    /**
     * Find all transaction logs for a specific user
     */
    List<TransactionLog> findByUserId(Long userId);
    
    /**
     * Find all transaction logs for a user with pagination
     */
    Page<TransactionLog> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find transaction logs for a user ordered by creation date descending
     */
    List<TransactionLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find transaction logs for a user by transaction type
     */
    List<TransactionLog> findByUserIdAndTransactionType(Long userId, String transactionType);
    
    /**
     * Find transaction logs for a user by transaction type with pagination
     */
    Page<TransactionLog> findByUserIdAndTransactionType(Long userId, String transactionType, Pageable pageable);
    
    /**
     * Find transaction logs for a user within a date range
     */
    List<TransactionLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find transaction logs for a user within a date range with pagination
     */
    Page<TransactionLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    /**
     * Find transaction logs with cash amounts (non-null and non-zero)
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId AND tl.amount IS NOT NULL AND tl.amount <> 0")
    List<TransactionLog> findCashTransactionsByUserId(@Param("userId") Long userId);
    
    /**
     * Find transaction logs with positive cash amounts (credits)
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId AND tl.amount > 0")
    List<TransactionLog> findCashCreditsByUserId(@Param("userId") Long userId);
    
    /**
     * Find transaction logs with negative cash amounts (debits)
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId AND tl.amount < 0")
    List<TransactionLog> findCashDebitsByUserId(@Param("userId") Long userId);
    
    /**
     * Get total cash credits for a user
     */
    @Query("SELECT COALESCE(SUM(tl.amount), 0) FROM TransactionLog tl WHERE tl.userId = :userId AND tl.amount > 0")
    BigDecimal getTotalCashCreditsForUser(@Param("userId") Long userId);
    
    /**
     * Get total cash debits for a user (returns positive value)
     */
    @Query("SELECT COALESCE(ABS(SUM(tl.amount)), 0) FROM TransactionLog tl WHERE tl.userId = :userId AND tl.amount < 0")
    BigDecimal getTotalCashDebitsForUser(@Param("userId") Long userId);
    
    /**
     * Get net cash amount for a user (credits - debits)
     */
    @Query("SELECT COALESCE(SUM(tl.amount), 0) FROM TransactionLog tl WHERE tl.userId = :userId AND tl.amount IS NOT NULL")
    BigDecimal getNetCashAmountForUser(@Param("userId") Long userId);
    
    /**
     * Count transactions by user and type
     */
    long countByUserIdAndTransactionType(Long userId, String transactionType);
    
    /**
     * Count transactions by user within date range
     */
    long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find recent transactions for a user (last N transactions)
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId ORDER BY tl.createdAt DESC")
    List<TransactionLog> findRecentTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find transactions by multiple transaction types
     */
    List<TransactionLog> findByUserIdAndTransactionTypeIn(Long userId, List<String> transactionTypes);
    
    /**
     * Find transactions containing specific text in description
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId AND UPPER(tl.description) LIKE UPPER(CONCAT('%', :searchText, '%'))")
    List<TransactionLog> findByUserIdAndDescriptionContaining(@Param("userId") Long userId, @Param("searchText") String searchText);
    
    /**
     * Find transactions with amount greater than specified value
     */
    List<TransactionLog> findByUserIdAndAmountGreaterThan(Long userId, BigDecimal minAmount);
    
    /**
     * Find transactions with amount less than specified value
     */
    List<TransactionLog> findByUserIdAndAmountLessThan(Long userId, BigDecimal maxAmount);
    
    /**
     * Find transactions with amount between specified range
     */
    List<TransactionLog> findByUserIdAndAmountBetween(Long userId, BigDecimal minAmount, BigDecimal maxAmount);
    
    /**
     * Get transaction statistics for a user within date range
     */
    @Query("SELECT " +
           "COUNT(tl) as totalTransactions, " +
           "COALESCE(SUM(CASE WHEN tl.amount > 0 THEN tl.amount ELSE 0 END), 0) as totalCredits, " +
           "COALESCE(ABS(SUM(CASE WHEN tl.amount < 0 THEN tl.amount ELSE 0 END)), 0) as totalDebits, " +
           "COALESCE(SUM(tl.amount), 0) as netAmount " +
           "FROM TransactionLog tl WHERE tl.userId = :userId AND tl.createdAt BETWEEN :startDate AND :endDate")
    Object[] getTransactionStatistics(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find all transactions for multiple users (for admin/reporting purposes)
     */
    List<TransactionLog> findByUserIdIn(List<Long> userIds);
    
    /**
     * Find transactions by type across all users (for admin/reporting purposes)
     */
    List<TransactionLog> findByTransactionType(String transactionType);
    
    /**
     * Get daily transaction summary for a user
     */
    @Query("SELECT DATE(tl.createdAt) as transactionDate, COUNT(tl) as transactionCount, COALESCE(SUM(tl.amount), 0) as totalAmount " +
           "FROM TransactionLog tl WHERE tl.userId = :userId AND tl.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(tl.createdAt) ORDER BY DATE(tl.createdAt) DESC")
    List<Object[]> getDailyTransactionSummary(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Check if user has any transactions
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Find first transaction for a user
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId ORDER BY tl.createdAt ASC")
    List<TransactionLog> findFirstTransactionByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find last transaction for a user
     */
    @Query("SELECT tl FROM TransactionLog tl WHERE tl.userId = :userId ORDER BY tl.createdAt DESC")
    List<TransactionLog> findLastTransactionByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Count all transactions for a user
     */
    long countByUserId(Long userId);
    
    /**
     * Find transaction logs for a user by transaction type ordered by creation date descending
     */
    List<TransactionLog> findByUserIdAndTransactionTypeOrderByCreatedAtDesc(Long userId, String transactionType);
    
    /**
     * Find transaction logs for a user within a date range ordered by creation date descending
     */
    List<TransactionLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find transaction logs with positive amounts ordered by creation date descending
     */
    List<TransactionLog> findByUserIdAndAmountGreaterThanOrderByCreatedAtDesc(Long userId, BigDecimal minAmount);
    
    /**
     * Find transaction logs with negative amounts ordered by creation date descending
     */
    List<TransactionLog> findByUserIdAndAmountLessThanOrderByCreatedAtDesc(Long userId, BigDecimal maxAmount);
    
    /**
     * Sum all amounts for a user
     */
    @Query("SELECT COALESCE(SUM(tl.amount), 0) FROM TransactionLog tl WHERE tl.userId = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);
    
    /**
     * Sum amounts for a user by transaction type
     */
    @Query("SELECT COALESCE(SUM(tl.amount), 0) FROM TransactionLog tl WHERE tl.userId = :userId AND tl.transactionType = :transactionType")
    BigDecimal sumAmountByUserIdAndTransactionType(@Param("userId") Long userId, @Param("transactionType") String transactionType);
    
    /**
     * Check if user has transactions of a specific type
     */
    boolean existsByUserIdAndTransactionType(Long userId, String transactionType);
}