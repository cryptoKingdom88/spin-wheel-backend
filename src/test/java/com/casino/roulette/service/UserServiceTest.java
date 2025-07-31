package com.casino.roulette.service;

import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TransactionLogRepository transactionLogRepository;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private final Long TEST_USER_ID = 1L;
    
    @BeforeEach
    void setUp() {
        testUser = new User(TEST_USER_ID);
        testUser.setCashBalance(new BigDecimal("100.00"));
        testUser.setAvailableSpins(5);
        testUser.setFirstDepositBonusUsed(false);
    }
    
    @Test
    void getOrCreateUser_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.getOrCreateUser(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getCashBalance());
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void getOrCreateUser_NewUser_CreatesAndReturnsUser() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        User result = userService.getOrCreateUser(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void getOrCreateUser_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.getOrCreateUser(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }
    
    @Test
    void updateCashBalance_PositiveAmount_UpdatesBalanceAndLogsTransaction() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateCashBalance(TEST_USER_ID, amount)).thenReturn(1);
        
        // When
        userService.updateCashBalance(TEST_USER_ID, amount);
        
        // Then
        verify(userRepository).updateCashBalance(TEST_USER_ID, amount);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals("BALANCE_UPDATE", savedLog.getTransactionType());
        assertEquals(amount, savedLog.getAmount());
        assertTrue(savedLog.getDescription().contains("increased"));
    }
    
    @Test
    void updateCashBalance_NegativeAmount_UpdatesBalanceAndLogsTransaction() {
        // Given
        BigDecimal amount = new BigDecimal("-25.00");
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateCashBalance(TEST_USER_ID, amount)).thenReturn(1);
        
        // When
        userService.updateCashBalance(TEST_USER_ID, amount);
        
        // Then
        verify(userRepository).updateCashBalance(TEST_USER_ID, amount);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals("BALANCE_UPDATE", savedLog.getTransactionType());
        assertEquals(amount, savedLog.getAmount());
        assertTrue(savedLog.getDescription().contains("decreased"));
    }
    
    @Test
    void updateCashBalance_InsufficientBalance_ThrowsException() {
        // Given
        BigDecimal amount = new BigDecimal("-150.00"); // More than current balance
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateCashBalance(TEST_USER_ID, amount)
        );
        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(userRepository, never()).updateCashBalance(any(), any());
        verify(transactionLogRepository, never()).save(any());
    }
    
    @Test
    void updateCashBalance_NullAmount_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateCashBalance(TEST_USER_ID, null)
        );
        assertEquals("Amount cannot be null", exception.getMessage());
    }
    
    @Test
    void updateCashBalance_UpdateFails_ThrowsException() {
        // Given
        BigDecimal amount = new BigDecimal("50.00");
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateCashBalance(TEST_USER_ID, amount)).thenReturn(0);
        
        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.updateCashBalance(TEST_USER_ID, amount)
        );
        assertTrue(exception.getMessage().contains("Failed to update cash balance"));
    }
    
    @Test
    void grantDailyLoginSpin_FirstTimeToday_GrantsSpinAndReturnsTrue() {
        // Given
        testUser.setLastDailyLogin(null); // Never logged in
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateAvailableSpins(TEST_USER_ID, 1)).thenReturn(1);
        when(userRepository.updateLastDailyLogin(eq(TEST_USER_ID), any(LocalDateTime.class))).thenReturn(1);
        
        // When
        boolean result = userService.grantDailyLoginSpin(TEST_USER_ID);
        
        // Then
        assertTrue(result);
        verify(userRepository).updateAvailableSpins(TEST_USER_ID, 1);
        verify(userRepository).updateLastDailyLogin(eq(TEST_USER_ID), any(LocalDateTime.class));
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals(TransactionLog.TYPE_DAILY_LOGIN_SPIN, savedLog.getTransactionType());
    }
    
    @Test
    void grantDailyLoginSpin_AlreadyLoggedInToday_ReturnsFalse() {
        // Given
        testUser.setLastDailyLogin(LocalDateTime.now()); // Already logged in today
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        // When
        boolean result = userService.grantDailyLoginSpin(TEST_USER_ID);
        
        // Then
        assertFalse(result);
        verify(userRepository, never()).updateAvailableSpins(any(), any());
        verify(userRepository, never()).updateLastDailyLogin(any(), any());
        verify(transactionLogRepository, never()).save(any());
    }
    
    @Test
    void grantDailyLoginSpin_LoggedInYesterday_GrantsSpinAndReturnsTrue() {
        // Given
        testUser.setLastDailyLogin(LocalDateTime.now().minusDays(1)); // Yesterday
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateAvailableSpins(TEST_USER_ID, 1)).thenReturn(1);
        when(userRepository.updateLastDailyLogin(eq(TEST_USER_ID), any(LocalDateTime.class))).thenReturn(1);
        
        // When
        boolean result = userService.grantDailyLoginSpin(TEST_USER_ID);
        
        // Then
        assertTrue(result);
        verify(userRepository).updateAvailableSpins(TEST_USER_ID, 1);
        verify(userRepository).updateLastDailyLogin(eq(TEST_USER_ID), any(LocalDateTime.class));
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void processFirstDepositBonus_EligibleUser_GrantsSpinAndMarksUsed() {
        // Given
        testUser.setFirstDepositBonusUsed(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateAvailableSpins(TEST_USER_ID, 1)).thenReturn(1);
        when(userRepository.markFirstDepositBonusUsed(TEST_USER_ID)).thenReturn(1);
        
        // When
        userService.processFirstDepositBonus(TEST_USER_ID);
        
        // Then
        verify(userRepository).updateAvailableSpins(TEST_USER_ID, 1);
        verify(userRepository).markFirstDepositBonusUsed(TEST_USER_ID);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals(TransactionLog.TYPE_FIRST_DEPOSIT_SPIN, savedLog.getTransactionType());
    }
    
    @Test
    void processFirstDepositBonus_AlreadyUsed_DoesNothing() {
        // Given
        testUser.setFirstDepositBonusUsed(true);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        // When
        userService.processFirstDepositBonus(TEST_USER_ID);
        
        // Then
        verify(userRepository, never()).updateAvailableSpins(any(), any());
        verify(userRepository, never()).markFirstDepositBonusUsed(any());
        verify(transactionLogRepository, never()).save(any());
    }
    
    @Test
    void hasSufficientSpins_SufficientSpins_ReturnsTrue() {
        // Given
        when(userRepository.hasSufficientSpins(TEST_USER_ID, 3)).thenReturn(true);
        
        // When
        boolean result = userService.hasSufficientSpins(TEST_USER_ID, 3);
        
        // Then
        assertTrue(result);
        verify(userRepository).hasSufficientSpins(TEST_USER_ID, 3);
    }
    
    @Test
    void hasSufficientSpins_InsufficientSpins_ReturnsFalse() {
        // Given
        when(userRepository.hasSufficientSpins(TEST_USER_ID, 10)).thenReturn(false);
        
        // When
        boolean result = userService.hasSufficientSpins(TEST_USER_ID, 10);
        
        // Then
        assertFalse(result);
        verify(userRepository).hasSufficientSpins(TEST_USER_ID, 10);
    }
    
    @Test
    void consumeSpins_SufficientSpins_ConsumesAndReturnsTrue() {
        // Given
        when(userRepository.consumeSpins(TEST_USER_ID, 2)).thenReturn(1);
        
        // When
        boolean result = userService.consumeSpins(TEST_USER_ID, 2);
        
        // Then
        assertTrue(result);
        verify(userRepository).consumeSpins(TEST_USER_ID, 2);
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void consumeSpins_InsufficientSpins_ReturnsFalse() {
        // Given
        when(userRepository.consumeSpins(TEST_USER_ID, 10)).thenReturn(0);
        
        // When
        boolean result = userService.consumeSpins(TEST_USER_ID, 10);
        
        // Then
        assertFalse(result);
        verify(userRepository).consumeSpins(TEST_USER_ID, 10);
        verify(transactionLogRepository, never()).save(any(TransactionLog.class));
    }
    
    @Test
    void grantSpins_ValidInput_GrantsSpinsAndLogsTransaction() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.updateAvailableSpins(TEST_USER_ID, 3)).thenReturn(1);
        
        // When
        userService.grantSpins(TEST_USER_ID, 3, "Mission reward");
        
        // Then
        verify(userRepository).updateAvailableSpins(TEST_USER_ID, 3);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals("SPINS_GRANTED", savedLog.getTransactionType());
        assertTrue(savedLog.getDescription().contains("Mission reward"));
    }
    
    @Test
    void grantSpins_NullReason_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.grantSpins(TEST_USER_ID, 3, null)
        );
        assertEquals("Reason cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void grantSpins_EmptyReason_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.grantSpins(TEST_USER_ID, 3, "   ")
        );
        assertEquals("Reason cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void getUser_ExistingUser_ReturnsUser() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        
        // When
        User result = userService.getUser(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
        verify(userRepository).findById(TEST_USER_ID);
    }
    
    @Test
    void getUser_NonExistingUser_ReturnsNull() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());
        
        // When
        User result = userService.getUser(TEST_USER_ID);
        
        // Then
        assertNull(result);
        verify(userRepository).findById(TEST_USER_ID);
    }
}