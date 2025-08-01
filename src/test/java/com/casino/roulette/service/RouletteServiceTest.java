package com.casino.roulette.service;

import com.casino.roulette.dto.SpinResultDTO;
import com.casino.roulette.entity.RouletteSlot;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.RouletteSlotRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouletteServiceTest {
    
    @Mock
    private RouletteSlotRepository rouletteSlotRepository;
    
    @Mock
    private TransactionLogRepository transactionLogRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private LetterService letterService;
    
    @InjectMocks
    private RouletteService rouletteService;
    
    private User testUser;
    private RouletteSlot cashSlot;
    private RouletteSlot letterSlot;
    private final Long TEST_USER_ID = 1L;
    
    @BeforeEach
    void setUp() {
        testUser = new User(TEST_USER_ID);
        testUser.setAvailableSpins(5);
        testUser.setCashBalance(new BigDecimal("100.00"));
        
        cashSlot = new RouletteSlot(RouletteSlot.SlotType.CASH, "10.50", 10);
        cashSlot.setId(1L);
        cashSlot.setActive(true);
        
        letterSlot = new RouletteSlot(RouletteSlot.SlotType.LETTER, "A", 5);
        letterSlot.setId(2L);
        letterSlot.setActive(true);
    }
    
    @Test
    void spinRoulette_CashWin_ReturnsCorrectResult() {
        // Given
        List<RouletteSlot> activeSlots = Arrays.asList(cashSlot);
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(userService.getUser(TEST_USER_ID)).thenReturn(testUser);
        
        // When
        SpinResultDTO result = rouletteService.spinRoulette(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals("CASH", result.getType());
        assertEquals("10.50", result.getValue());
        assertEquals(new BigDecimal("10.50"), result.getCash());
        assertNull(result.getLetter());
        assertEquals(Integer.valueOf(5), result.getRemainingSpins());
        
        verify(userService).updateCashBalance(TEST_USER_ID, new BigDecimal("10.50"));
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void spinRoulette_LetterWin_ReturnsCorrectResult() {
        // Given
        List<RouletteSlot> activeSlots = Arrays.asList(letterSlot);
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(userService.getUser(TEST_USER_ID)).thenReturn(testUser);
        
        // When
        SpinResultDTO result = rouletteService.spinRoulette(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals("LETTER", result.getType());
        assertEquals("A", result.getValue());
        assertNull(result.getCash());
        assertEquals("A", result.getLetter());
        assertEquals(Integer.valueOf(5), result.getRemainingSpins());
        
        verify(letterService).addLetterToCollection(TEST_USER_ID, "A");
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void spinRoulette_WeightedSelection_SelectsCorrectly() {
        // Given - Create slots with different weights
        RouletteSlot heavyWeightSlot = new RouletteSlot(RouletteSlot.SlotType.CASH, "100.00", 90);
        RouletteSlot lightWeightSlot = new RouletteSlot(RouletteSlot.SlotType.CASH, "1.00", 10);
        List<RouletteSlot> activeSlots = Arrays.asList(heavyWeightSlot, lightWeightSlot);
        
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(userService.getUser(TEST_USER_ID)).thenReturn(testUser);
        
        // When - Spin multiple times to test weighted selection
        int heavyWeightWins = 0;
        int totalSpins = 100;
        
        for (int i = 0; i < totalSpins; i++) {
            SpinResultDTO result = rouletteService.spinRoulette(TEST_USER_ID);
            if ("100.00".equals(result.getValue())) {
                heavyWeightWins++;
            }
        }
        
        // Then - Heavy weight slot should win more often (allowing for randomness)
        // With 90% weight, we expect around 90 wins out of 100, but allow for variance
        assertTrue(heavyWeightWins > 70, "Heavy weight slot should win more often. Actual wins: " + heavyWeightWins);
    }
    
    @Test
    void spinRoulette_InsufficientSpins_ThrowsException() {
        // Given
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(false);
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> rouletteService.spinRoulette(TEST_USER_ID)
        );
        assertEquals("User has no available spins", exception.getMessage());
        
        verify(userService, never()).consumeSpins(any(), any());
        verify(rouletteSlotRepository, never()).findByActiveTrue();
    }
    
    @Test
    void spinRoulette_NoActiveSlots_ThrowsException() {
        // Given
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> rouletteService.spinRoulette(TEST_USER_ID)
        );
        assertEquals("No active roulette slots available", exception.getMessage());
    }
    
    @Test
    void spinRoulette_FailedToConsumeSpins_ThrowsException() {
        // Given
        List<RouletteSlot> activeSlots = Arrays.asList(cashSlot);
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(false);
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> rouletteService.spinRoulette(TEST_USER_ID)
        );
        assertEquals("Failed to consume spin", exception.getMessage());
    }
    
    @Test
    void spinRoulette_InvalidCashValue_ThrowsException() {
        // Given
        RouletteSlot invalidCashSlot = new RouletteSlot(RouletteSlot.SlotType.CASH, "invalid", 10);
        List<RouletteSlot> activeSlots = Arrays.asList(invalidCashSlot);
        
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(userService.getUser(TEST_USER_ID)).thenReturn(testUser);
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> rouletteService.spinRoulette(TEST_USER_ID)
        );
        assertTrue(exception.getMessage().contains("Invalid cash value in slot"));
    }
    
    @Test
    void spinRoulette_InvalidLetterValue_ThrowsException() {
        // Given
        RouletteSlot invalidLetterSlot = new RouletteSlot(RouletteSlot.SlotType.LETTER, "123", 10);
        List<RouletteSlot> activeSlots = Arrays.asList(invalidLetterSlot);
        
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(userService.getUser(TEST_USER_ID)).thenReturn(testUser);
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> rouletteService.spinRoulette(TEST_USER_ID)
        );
        assertTrue(exception.getMessage().contains("Invalid letter value in slot"));
    }
    
    @Test
    void spinRoulette_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> rouletteService.spinRoulette(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }
    
    @Test
    void getRouletteConfiguration_ReturnsActiveSlots() {
        // Given
        List<RouletteSlot> expectedSlots = Arrays.asList(cashSlot, letterSlot);
        when(rouletteSlotRepository.findActiveSlotsByWeightDesc()).thenReturn(expectedSlots);
        
        // When
        List<RouletteSlot> result = rouletteService.getRouletteConfiguration();
        
        // Then
        assertEquals(expectedSlots, result);
        verify(rouletteSlotRepository).findActiveSlotsByWeightDesc();
    }
    
    @Test
    void updateRouletteSlots_ValidSlots_SavesSuccessfully() {
        // Given
        List<RouletteSlot> slots = Arrays.asList(cashSlot, letterSlot);
        
        // When
        rouletteService.updateRouletteSlots(slots);
        
        // Then
        verify(rouletteSlotRepository).saveAll(slots);
    }
    
    @Test
    void updateRouletteSlots_NullSlots_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> rouletteService.updateRouletteSlots(null)
        );
        assertEquals("Slots list cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void updateRouletteSlots_EmptySlots_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> rouletteService.updateRouletteSlots(Collections.emptyList())
        );
        assertEquals("Slots list cannot be null or empty", exception.getMessage());
    }
    
    @Test
    void updateRouletteSlots_InvalidSlot_ThrowsException() {
        // Given
        RouletteSlot invalidSlot = new RouletteSlot();
        invalidSlot.setSlotType(null); // Invalid
        List<RouletteSlot> slots = Arrays.asList(invalidSlot);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> rouletteService.updateRouletteSlots(slots)
        );
        assertEquals("Slot type cannot be null", exception.getMessage());
    }
    
    @Test
    void isRouletteAvailable_WithActiveSlots_ReturnsTrue() {
        // Given
        when(rouletteSlotRepository.existsByActiveTrue()).thenReturn(true);
        
        // When
        boolean result = rouletteService.isRouletteAvailable();
        
        // Then
        assertTrue(result);
        verify(rouletteSlotRepository).existsByActiveTrue();
    }
    
    @Test
    void isRouletteAvailable_NoActiveSlots_ReturnsFalse() {
        // Given
        when(rouletteSlotRepository.existsByActiveTrue()).thenReturn(false);
        
        // When
        boolean result = rouletteService.isRouletteAvailable();
        
        // Then
        assertFalse(result);
        verify(rouletteSlotRepository).existsByActiveTrue();
    }
    
    @Test
    void getTotalActiveWeight_ReturnsRepositoryResult() {
        // Given
        Long expectedWeight = 100L;
        when(rouletteSlotRepository.getTotalWeightForActiveSlots()).thenReturn(expectedWeight);
        
        // When
        Long result = rouletteService.getTotalActiveWeight();
        
        // Then
        assertEquals(expectedWeight, result);
        verify(rouletteSlotRepository).getTotalWeightForActiveSlots();
    }
    
    @Test
    void getActiveSlotCountByType_ValidType_ReturnsCount() {
        // Given
        long expectedCount = 5L;
        when(rouletteSlotRepository.countByActiveTrueAndSlotType(RouletteSlot.SlotType.CASH))
            .thenReturn(expectedCount);
        
        // When
        long result = rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.CASH);
        
        // Then
        assertEquals(expectedCount, result);
        verify(rouletteSlotRepository).countByActiveTrueAndSlotType(RouletteSlot.SlotType.CASH);
    }
    
    @Test
    void getActiveSlotCountByType_NullType_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> rouletteService.getActiveSlotCountByType(null)
        );
        assertEquals("Slot type cannot be null", exception.getMessage());
    }
    
    @Test
    void spinRoulette_LogsTransactionCorrectly() {
        // Given
        List<RouletteSlot> activeSlots = Arrays.asList(cashSlot);
        when(userService.hasSufficientSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(rouletteSlotRepository.findByActiveTrue()).thenReturn(activeSlots);
        when(userService.consumeSpins(TEST_USER_ID, 1)).thenReturn(true);
        when(userService.getUser(TEST_USER_ID)).thenReturn(testUser);
        
        // When
        rouletteService.spinRoulette(TEST_USER_ID);
        
        // Then
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals("ROULETTE_SPIN", savedLog.getTransactionType());
        assertTrue(savedLog.getDescription().contains("CASH"));
        assertTrue(savedLog.getDescription().contains("10.50"));
    }
}