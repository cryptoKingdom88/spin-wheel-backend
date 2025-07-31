package com.casino.roulette.service;

import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    
    @Mock
    private TransactionLogRepository transactionLogRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private TransactionService transactionService;
    
    private User testUser;
    private TransactionLog testTransaction;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_TRANSACTION_ID = 1L;
    
    @BeforeEach
    void setUp() {
        testUser = new User(TEST_USER_ID);
        testUser.setCashBalance(new BigDecimal("100.00"));
        
        testTransaction = new TransactionLog(TEST_USER_ID, "DEPOSIT", new BigDecimal("50.00"), "Test deposit");
        testTransaction.setId(TEST_TRANSACTION_ID);
    }
    
    @Test
    void logTransaction_WithoutAmount_CreatesTransaction() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        
        // When
        TransactionLog result = transactionService.logTransaction(TEST_USER_ID, "SPIN_CONSUMED", "Free spin consumed");
        
        // Then
        assertNotNull(result);
        verify(userService).getOrCreateUser(TEST_USER_ID);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals("SPIN_CONSUMED", savedLog.getTransactionType());
        assertEquals("Free spin consumed", savedLog.getDescription());
        assertNull(savedLog.getAmount());
    }
    
    @Test
    void logTransaction_WithAmount_CreatesTransactionWithAmount() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(transactionLogRepository.save(any(TransactionLog.class))).thenReturn(testTransaction);
        
        // When
        TransactionLog result = transactionService.logTransaction(TEST_USER_ID, "DEPOSIT", new BigDecimal("50.00"), "Test deposit");
        
        // Then
        assertNotNull(result);
        verify(userService).getOrCreateUser(TEST_USER_ID);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals("DEPOSIT", savedLog.getTransactionType());
        assertEquals(new BigDecimal("50.00"), savedLog.getAmount());
        assertEquals("Test deposit", savedLog.getDescription());
    }
    
    @Test
    void logTransaction_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.logTransaction(null, "DEPOSIT", "Test")
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }
    
    @Test
    void logTransaction_NullTransactionType_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.logTransaction(TEST_USER_ID, null, "Test")
        );
        assertEquals("Transaction type cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void logTransaction_EmptyDescription_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.logTransaction(TEST_USER_ID, "DEPOSIT", "")
        );
        assertEquals("Description cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void getTransactionHistory_Paginated_ReturnsPagedResults() {
        // Given
        List<TransactionLog> transactions = Arrays.asList(testTransaction);
        Page<TransactionLog> page = new PageImpl<>(transactions);
        when(transactionLogRepository.findByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(page);
        
        // When
        Page<TransactionLog> result = transactionService.getTransactionHistory(TEST_USER_ID, 0, 10);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testTransaction, result.getContent().get(0));
        
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionLogRepository).findByUserId(eq(TEST_USER_ID), pageableCaptor.capture());
        
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }
    
    @Test
    void getTransactionHistory_All_ReturnsAllTransactions() {
        // Given
        List<TransactionLog> transactions = Arrays.asList(testTransaction);
        when(transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)).thenReturn(transactions);
        
        // When
        List<TransactionLog> result = transactionService.getTransactionHistory(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction, result.get(0));
        verify(transactionLogRepository).findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
    }
    
    @Test
    void getTransactionHistoryByType_ReturnsFilteredTransactions() {
        // Given
        List<TransactionLog> transactions = Arrays.asList(testTransaction);
        when(transactionLogRepository.findByUserIdAndTransactionTypeOrderByCreatedAtDesc(TEST_USER_ID, "DEPOSIT"))
            .thenReturn(transactions);
        
        // When
        List<TransactionLog> result = transactionService.getTransactionHistoryByType(TEST_USER_ID, "DEPOSIT");
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction, result.get(0));
        verify(transactionLogRepository).findByUserIdAndTransactionTypeOrderByCreatedAtDesc(TEST_USER_ID, "DEPOSIT");
    }
    
    @Test
    void getTransactionHistoryByDateRange_ReturnsFilteredTransactions() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<TransactionLog> transactions = Arrays.asList(testTransaction);
        when(transactionLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(TEST_USER_ID, startDate, endDate))
            .thenReturn(transactions);
        
        // When
        List<TransactionLog> result = transactionService.getTransactionHistoryByDateRange(TEST_USER_ID, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction, result.get(0));
        verify(transactionLogRepository).findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(TEST_USER_ID, startDate, endDate);
    }
    
    @Test
    void getTransactionHistoryByDateRange_InvalidDateRange_ThrowsException() {
        // Given
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().minusDays(1); // End before start
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getTransactionHistoryByDateRange(TEST_USER_ID, startDate, endDate)
        );
        assertEquals("Start date cannot be after end date", exception.getMessage());
    }
    
    @Test
    void getRecentTransactions_ReturnsLimitedResults() {
        // Given
        List<TransactionLog> transactions = Arrays.asList(testTransaction);
        Page<TransactionLog> page = new PageImpl<>(transactions);
        when(transactionLogRepository.findByUserId(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(page);
        
        // When
        List<TransactionLog> result = transactionService.getRecentTransactions(TEST_USER_ID, 5);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction, result.get(0));
        
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionLogRepository).findByUserId(eq(TEST_USER_ID), pageableCaptor.capture());
        
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
    }
    
    @Test
    void getTransactionById_ExistingTransaction_ReturnsTransaction() {
        // Given
        when(transactionLogRepository.findById(TEST_TRANSACTION_ID)).thenReturn(Optional.of(testTransaction));
        
        // When
        Optional<TransactionLog> result = transactionService.getTransactionById(TEST_TRANSACTION_ID);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(testTransaction, result.get());
        verify(transactionLogRepository).findById(TEST_TRANSACTION_ID);
    }
    
    @Test
    void getTransactionById_NonExistingTransaction_ReturnsEmpty() {
        // Given
        when(transactionLogRepository.findById(TEST_TRANSACTION_ID)).thenReturn(Optional.empty());
        
        // When
        Optional<TransactionLog> result = transactionService.getTransactionById(TEST_TRANSACTION_ID);
        
        // Then
        assertFalse(result.isPresent());
        verify(transactionLogRepository).findById(TEST_TRANSACTION_ID);
    }
    
    @Test
    void getTransactionCount_ReturnsRepositoryResult() {
        // Given
        when(transactionLogRepository.countByUserId(TEST_USER_ID)).thenReturn(5L);
        
        // When
        long result = transactionService.getTransactionCount(TEST_USER_ID);
        
        // Then
        assertEquals(5L, result);
        verify(transactionLogRepository).countByUserId(TEST_USER_ID);
    }
    
    @Test
    void getTransactionCountByType_ReturnsRepositoryResult() {
        // Given
        when(transactionLogRepository.countByUserIdAndTransactionType(TEST_USER_ID, "DEPOSIT")).thenReturn(3L);
        
        // When
        long result = transactionService.getTransactionCountByType(TEST_USER_ID, "DEPOSIT");
        
        // Then
        assertEquals(3L, result);
        verify(transactionLogRepository).countByUserIdAndTransactionType(TEST_USER_ID, "DEPOSIT");
    }
    
    @Test
    void getTotalCashAmount_ReturnsRepositoryResult() {
        // Given
        when(transactionLogRepository.sumAmountByUserId(TEST_USER_ID)).thenReturn(new BigDecimal("150.00"));
        
        // When
        BigDecimal result = transactionService.getTotalCashAmount(TEST_USER_ID);
        
        // Then
        assertEquals(new BigDecimal("150.00"), result);
        verify(transactionLogRepository).sumAmountByUserId(TEST_USER_ID);
    }
    
    @Test
    void getTotalCashAmount_NullResult_ReturnsZero() {
        // Given
        when(transactionLogRepository.sumAmountByUserId(TEST_USER_ID)).thenReturn(null);
        
        // When
        BigDecimal result = transactionService.getTotalCashAmount(TEST_USER_ID);
        
        // Then
        assertEquals(BigDecimal.ZERO, result);
    }
    
    @Test
    void getTotalCashAmountByType_ReturnsRepositoryResult() {
        // Given
        when(transactionLogRepository.sumAmountByUserIdAndTransactionType(TEST_USER_ID, "DEPOSIT"))
            .thenReturn(new BigDecimal("100.00"));
        
        // When
        BigDecimal result = transactionService.getTotalCashAmountByType(TEST_USER_ID, "DEPOSIT");
        
        // Then
        assertEquals(new BigDecimal("100.00"), result);
        verify(transactionLogRepository).sumAmountByUserIdAndTransactionType(TEST_USER_ID, "DEPOSIT");
    }
    
    @Test
    void getCashCreditTransactions_ReturnsPositiveAmountTransactions() {
        // Given
        List<TransactionLog> transactions = Arrays.asList(testTransaction);
        when(transactionLogRepository.findByUserIdAndAmountGreaterThanOrderByCreatedAtDesc(TEST_USER_ID, BigDecimal.ZERO))
            .thenReturn(transactions);
        
        // When
        List<TransactionLog> result = transactionService.getCashCreditTransactions(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTransaction, result.get(0));
        verify(transactionLogRepository).findByUserIdAndAmountGreaterThanOrderByCreatedAtDesc(TEST_USER_ID, BigDecimal.ZERO);
    }
    
    @Test
    void getCashDebitTransactions_ReturnsNegativeAmountTransactions() {
        // Given
        TransactionLog debitTransaction = new TransactionLog(TEST_USER_ID, "WITHDRAWAL", new BigDecimal("-25.00"), "Test withdrawal");
        List<TransactionLog> transactions = Arrays.asList(debitTransaction);
        when(transactionLogRepository.findByUserIdAndAmountLessThanOrderByCreatedAtDesc(TEST_USER_ID, BigDecimal.ZERO))
            .thenReturn(transactions);
        
        // When
        List<TransactionLog> result = transactionService.getCashDebitTransactions(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(debitTransaction, result.get(0));
        verify(transactionLogRepository).findByUserIdAndAmountLessThanOrderByCreatedAtDesc(TEST_USER_ID, BigDecimal.ZERO);
    }
    
    @Test
    void hasTransactions_WithTransactions_ReturnsTrue() {
        // Given
        when(transactionLogRepository.existsByUserId(TEST_USER_ID)).thenReturn(true);
        
        // When
        boolean result = transactionService.hasTransactions(TEST_USER_ID);
        
        // Then
        assertTrue(result);
        verify(transactionLogRepository).existsByUserId(TEST_USER_ID);
    }
    
    @Test
    void hasTransactions_NoTransactions_ReturnsFalse() {
        // Given
        when(transactionLogRepository.existsByUserId(TEST_USER_ID)).thenReturn(false);
        
        // When
        boolean result = transactionService.hasTransactions(TEST_USER_ID);
        
        // Then
        assertFalse(result);
        verify(transactionLogRepository).existsByUserId(TEST_USER_ID);
    }
    
    @Test
    void hasTransactionsOfType_WithTransactionsOfType_ReturnsTrue() {
        // Given
        when(transactionLogRepository.existsByUserIdAndTransactionType(TEST_USER_ID, "DEPOSIT")).thenReturn(true);
        
        // When
        boolean result = transactionService.hasTransactionsOfType(TEST_USER_ID, "DEPOSIT");
        
        // Then
        assertTrue(result);
        verify(transactionLogRepository).existsByUserIdAndTransactionType(TEST_USER_ID, "DEPOSIT");
    }
    
    @Test
    void hasTransactionsOfType_NoTransactionsOfType_ReturnsFalse() {
        // Given
        when(transactionLogRepository.existsByUserIdAndTransactionType(TEST_USER_ID, "WITHDRAWAL")).thenReturn(false);
        
        // When
        boolean result = transactionService.hasTransactionsOfType(TEST_USER_ID, "WITHDRAWAL");
        
        // Then
        assertFalse(result);
        verify(transactionLogRepository).existsByUserIdAndTransactionType(TEST_USER_ID, "WITHDRAWAL");
    }
    
    @Test
    void getTransactionStatistics_ReturnsCompleteStatistics() {
        // Given
        when(transactionLogRepository.countByUserId(TEST_USER_ID)).thenReturn(10L);
        when(transactionLogRepository.sumAmountByUserId(TEST_USER_ID)).thenReturn(new BigDecimal("200.00"));
        when(transactionLogRepository.countByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_DEPOSIT)).thenReturn(3L);
        when(transactionLogRepository.sumAmountByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_DEPOSIT))
            .thenReturn(new BigDecimal("150.00"));
        when(transactionLogRepository.countByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_ROULETTE_WIN)).thenReturn(2L);
        when(transactionLogRepository.sumAmountByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_ROULETTE_WIN))
            .thenReturn(new BigDecimal("50.00"));
        when(transactionLogRepository.countByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_LETTER_BONUS)).thenReturn(1L);
        when(transactionLogRepository.sumAmountByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_LETTER_BONUS))
            .thenReturn(new BigDecimal("25.00"));
        when(transactionLogRepository.countByUserIdAndTransactionType(TEST_USER_ID, TransactionLog.TYPE_SPIN_CONSUMED)).thenReturn(5L);
        
        // When
        TransactionService.TransactionStatistics result = transactionService.getTransactionStatistics(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(10L, result.getTotalTransactions());
        assertEquals(new BigDecimal("200.00"), result.getTotalAmount());
        assertEquals(3L, result.getDepositCount());
        assertEquals(new BigDecimal("150.00"), result.getTotalDeposits());
        assertEquals(2L, result.getRouletteWinCount());
        assertEquals(new BigDecimal("50.00"), result.getTotalRouletteWins());
        assertEquals(1L, result.getLetterBonusCount());
        assertEquals(new BigDecimal("25.00"), result.getTotalLetterBonuses());
        assertEquals(5L, result.getTotalSpins());
    }
    
    @Test
    void getTransactionHistory_InvalidPageNumber_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getTransactionHistory(TEST_USER_ID, -1, 10)
        );
        assertEquals("Page number cannot be negative", exception.getMessage());
    }
    
    @Test
    void getTransactionHistory_InvalidPageSize_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getTransactionHistory(TEST_USER_ID, 0, 0)
        );
        assertEquals("Page size must be positive", exception.getMessage());
    }
    
    @Test
    void getRecentTransactions_InvalidLimit_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.getRecentTransactions(TEST_USER_ID, 0)
        );
        assertEquals("Limit must be positive", exception.getMessage());
    }
}