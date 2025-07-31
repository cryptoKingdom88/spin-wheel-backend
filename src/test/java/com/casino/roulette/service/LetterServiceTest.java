package com.casino.roulette.service;

import com.casino.roulette.dto.LetterCollectionDTO;
import com.casino.roulette.dto.LetterWordDTO;
import com.casino.roulette.entity.LetterCollection;
import com.casino.roulette.entity.LetterWord;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.LetterCollectionRepository;
import com.casino.roulette.repository.LetterWordRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LetterServiceTest {
    
    @Mock
    private LetterCollectionRepository letterCollectionRepository;
    
    @Mock
    private LetterWordRepository letterWordRepository;
    
    @Mock
    private TransactionLogRepository transactionLogRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private LetterService letterService;
    
    private User testUser;
    private LetterCollection testCollection;
    private LetterWord testWord;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_WORD_ID = 1L;
    
    @BeforeEach
    void setUp() {
        testUser = new User(TEST_USER_ID);
        testUser.setCashBalance(new BigDecimal("100.00"));
        
        testCollection = new LetterCollection(TEST_USER_ID, "A", 3);
        testCollection.setId(1L);
        
        // Create a word that requires A:2, B:1
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 2);
        requiredLetters.put("B", 1);
        testWord = new LetterWord("AB", requiredLetters, new BigDecimal("50.00"));
        testWord.setId(TEST_WORD_ID);
        testWord.setActive(true);
    }
    
    @Test
    void addLetterToCollection_ExistingCollection_IncrementsCount() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(letterCollectionRepository.findByUserIdAndLetter(TEST_USER_ID, "A"))
            .thenReturn(Optional.of(testCollection));
        when(letterCollectionRepository.incrementExistingLetterCount(TEST_USER_ID, "A", 1))
            .thenReturn(1);
        
        // When
        letterService.addLetterToCollection(TEST_USER_ID, "a");
        
        // Then
        verify(letterCollectionRepository).incrementExistingLetterCount(TEST_USER_ID, "A", 1);
        verify(letterCollectionRepository, never()).save(any(LetterCollection.class));
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals(TransactionLog.TYPE_LETTER_COLLECTED, savedLog.getTransactionType());
    }
    
    @Test
    void addLetterToCollection_NewCollection_CreatesNew() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(letterCollectionRepository.findByUserIdAndLetter(TEST_USER_ID, "B"))
            .thenReturn(Optional.empty());
        
        // When
        letterService.addLetterToCollection(TEST_USER_ID, "b");
        
        // Then
        ArgumentCaptor<LetterCollection> collectionCaptor = ArgumentCaptor.forClass(LetterCollection.class);
        verify(letterCollectionRepository).save(collectionCaptor.capture());
        
        LetterCollection savedCollection = collectionCaptor.getValue();
        assertEquals(TEST_USER_ID, savedCollection.getUserId());
        assertEquals("B", savedCollection.getLetter());
        assertEquals(Integer.valueOf(1), savedCollection.getCount());
        
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void addLetterToCollection_InvalidLetter_ThrowsException() {
        // When & Then
        IllegalArgumentException exception1 = assertThrows(
            IllegalArgumentException.class,
            () -> letterService.addLetterToCollection(TEST_USER_ID, "123")
        );
        assertTrue(exception1.getMessage().contains("single alphabetic character"));
        
        IllegalArgumentException exception2 = assertThrows(
            IllegalArgumentException.class,
            () -> letterService.addLetterToCollection(TEST_USER_ID, "AB")
        );
        assertTrue(exception2.getMessage().contains("single alphabetic character"));
        
        IllegalArgumentException exception3 = assertThrows(
            IllegalArgumentException.class,
            () -> letterService.addLetterToCollection(TEST_USER_ID, null)
        );
        assertTrue(exception3.getMessage().contains("cannot be null or empty"));
    }
    
    @Test
    void addLetterToCollection_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> letterService.addLetterToCollection(null, "A")
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }
    
    @Test
    void getUserLetterCollection_WithLetters_ReturnsCorrectDTOs() {
        // Given
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 3);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        List<LetterCollectionDTO> result = letterService.getUserLetterCollection(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        LetterCollectionDTO dtoA = result.stream()
            .filter(dto -> "A".equals(dto.getLetter()))
            .findFirst().orElse(null);
        assertNotNull(dtoA);
        assertEquals(Integer.valueOf(3), dtoA.getCount());
        
        LetterCollectionDTO dtoB = result.stream()
            .filter(dto -> "B".equals(dto.getLetter()))
            .findFirst().orElse(null);
        assertNotNull(dtoB);
        assertEquals(Integer.valueOf(1), dtoB.getCount());
    }
    
    @Test
    void getAvailableWords_WithSufficientLetters_ReturnsClaimableWords() {
        // Given
        List<LetterWord> activeWords = Arrays.asList(testWord);
        when(letterWordRepository.findActiveWordsOrderByRewardDesc()).thenReturn(activeWords);
        
        // User has A:3, B:1 - sufficient for word requiring A:2, B:1
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 3);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        List<LetterWordDTO> result = letterService.getAvailableWords(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        LetterWordDTO dto = result.get(0);
        assertEquals(TEST_WORD_ID, dto.getId());
        assertEquals("AB", dto.getWord());
        assertEquals(new BigDecimal("50.00"), dto.getRewardAmount());
        assertTrue(dto.getCanClaim());
    }
    
    @Test
    void getAvailableWords_InsufficientLetters_ReturnsNonClaimableWords() {
        // Given
        List<LetterWord> activeWords = Arrays.asList(testWord);
        when(letterWordRepository.findActiveWordsOrderByRewardDesc()).thenReturn(activeWords);
        
        // User has A:1, B:1 - insufficient for word requiring A:2, B:1
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 1);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        List<LetterWordDTO> result = letterService.getAvailableWords(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        LetterWordDTO dto = result.get(0);
        assertEquals(TEST_WORD_ID, dto.getId());
        assertFalse(dto.getCanClaim());
    }
    
    @Test
    void claimWordBonus_ValidClaim_DeductsLettersAndGrantsCash() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(letterWordRepository.findById(TEST_WORD_ID)).thenReturn(Optional.of(testWord));
        
        // User has sufficient letters
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 3);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        when(letterCollectionRepository.decrementLetterCount(TEST_USER_ID, "A", 2)).thenReturn(1);
        when(letterCollectionRepository.decrementLetterCount(TEST_USER_ID, "B", 1)).thenReturn(1);
        
        // When
        letterService.claimWordBonus(TEST_USER_ID, TEST_WORD_ID);
        
        // Then
        verify(letterCollectionRepository).decrementLetterCount(TEST_USER_ID, "A", 2);
        verify(letterCollectionRepository).decrementLetterCount(TEST_USER_ID, "B", 1);
        verify(userService).updateCashBalance(TEST_USER_ID, new BigDecimal("50.00"));
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals(TransactionLog.TYPE_LETTER_BONUS, savedLog.getTransactionType());
        assertEquals(new BigDecimal("50.00"), savedLog.getAmount());
    }
    
    @Test
    void claimWordBonus_InsufficientLetters_ThrowsException() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(letterWordRepository.findById(TEST_WORD_ID)).thenReturn(Optional.of(testWord));
        
        // User has insufficient letters (A:1, B:1 but needs A:2, B:1)
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 1);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> letterService.claimWordBonus(TEST_USER_ID, TEST_WORD_ID)
        );
        assertTrue(exception.getMessage().contains("does not have sufficient letters"));
        
        verify(letterCollectionRepository, never()).decrementLetterCount(any(), any(), any());
        verify(userService, never()).updateCashBalance(any(), any());
    }
    
    @Test
    void claimWordBonus_WordNotFound_ThrowsException() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(letterWordRepository.findById(TEST_WORD_ID)).thenReturn(Optional.empty());
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> letterService.claimWordBonus(TEST_USER_ID, TEST_WORD_ID)
        );
        assertTrue(exception.getMessage().contains("Word not found"));
    }
    
    @Test
    void claimWordBonus_InactiveWord_ThrowsException() {
        // Given
        testWord.setActive(false);
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(letterWordRepository.findById(TEST_WORD_ID)).thenReturn(Optional.of(testWord));
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> letterService.claimWordBonus(TEST_USER_ID, TEST_WORD_ID)
        );
        assertTrue(exception.getMessage().contains("Word is not active"));
    }
    
    @Test
    void canClaimWord_SufficientLetters_ReturnsTrue() {
        // Given
        when(letterWordRepository.findById(TEST_WORD_ID)).thenReturn(Optional.of(testWord));
        
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 3);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        boolean result = letterService.canClaimWord(TEST_USER_ID, TEST_WORD_ID);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void canClaimWord_InsufficientLetters_ReturnsFalse() {
        // Given
        when(letterWordRepository.findById(TEST_WORD_ID)).thenReturn(Optional.of(testWord));
        
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 1); // Insufficient
        List<LetterCollection> collections = Arrays.asList(collectionA);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        boolean result = letterService.canClaimWord(TEST_USER_ID, TEST_WORD_ID);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void getLetterCount_ExistingLetter_ReturnsCount() {
        // Given
        when(letterCollectionRepository.getLetterCountForUser(TEST_USER_ID, "A")).thenReturn(5);
        
        // When
        Integer result = letterService.getLetterCount(TEST_USER_ID, "a");
        
        // Then
        assertEquals(Integer.valueOf(5), result);
        verify(letterCollectionRepository).getLetterCountForUser(TEST_USER_ID, "A");
    }
    
    @Test
    void getLetterCount_NonExistingLetter_ReturnsZero() {
        // Given
        when(letterCollectionRepository.getLetterCountForUser(TEST_USER_ID, "Z")).thenReturn(0);
        
        // When
        Integer result = letterService.getLetterCount(TEST_USER_ID, "z");
        
        // Then
        assertEquals(Integer.valueOf(0), result);
    }
    
    @Test
    void hasAtLeastLetters_SufficientCount_ReturnsTrue() {
        // Given
        when(letterCollectionRepository.hasAtLeastLetterCount(TEST_USER_ID, "A", 3)).thenReturn(true);
        
        // When
        boolean result = letterService.hasAtLeastLetters(TEST_USER_ID, "a", 3);
        
        // Then
        assertTrue(result);
        verify(letterCollectionRepository).hasAtLeastLetterCount(TEST_USER_ID, "A", 3);
    }
    
    @Test
    void hasAtLeastLetters_InsufficientCount_ReturnsFalse() {
        // Given
        when(letterCollectionRepository.hasAtLeastLetterCount(TEST_USER_ID, "A", 5)).thenReturn(false);
        
        // When
        boolean result = letterService.hasAtLeastLetters(TEST_USER_ID, "a", 5);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void getTotalLetterCount_ReturnsRepositoryResult() {
        // Given
        when(letterCollectionRepository.getTotalLetterCountForUser(TEST_USER_ID)).thenReturn(10L);
        
        // When
        Long result = letterService.getTotalLetterCount(TEST_USER_ID);
        
        // Then
        assertEquals(Long.valueOf(10), result);
        verify(letterCollectionRepository).getTotalLetterCountForUser(TEST_USER_ID);
    }
    
    @Test
    void getDistinctLetters_ReturnsRepositoryResult() {
        // Given
        List<String> expectedLetters = Arrays.asList("A", "B", "C");
        when(letterCollectionRepository.getDistinctLettersForUser(TEST_USER_ID)).thenReturn(expectedLetters);
        
        // When
        List<String> result = letterService.getDistinctLetters(TEST_USER_ID);
        
        // Then
        assertEquals(expectedLetters, result);
        verify(letterCollectionRepository).getDistinctLettersForUser(TEST_USER_ID);
    }
    
    @Test
    void getClaimableWords_WithClaimableWords_ReturnsOnlyClaimable() {
        // Given
        List<LetterWord> activeWords = Arrays.asList(testWord);
        when(letterWordRepository.findActiveWordsOrderByRewardDesc()).thenReturn(activeWords);
        
        // User has sufficient letters
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 3);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        List<LetterWordDTO> result = letterService.getClaimableWords(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getCanClaim());
    }
    
    @Test
    void hasClaimableWords_WithClaimableWords_ReturnsTrue() {
        // Given
        List<LetterWord> activeWords = Arrays.asList(testWord);
        when(letterWordRepository.findActiveWordsOrderByRewardDesc()).thenReturn(activeWords);
        
        // User has sufficient letters
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 3);
        LetterCollection collectionB = new LetterCollection(TEST_USER_ID, "B", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA, collectionB);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        boolean result = letterService.hasClaimableWords(TEST_USER_ID);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void hasClaimableWords_NoClaimableWords_ReturnsFalse() {
        // Given
        List<LetterWord> activeWords = Arrays.asList(testWord);
        when(letterWordRepository.findActiveWordsOrderByRewardDesc()).thenReturn(activeWords);
        
        // User has insufficient letters
        LetterCollection collectionA = new LetterCollection(TEST_USER_ID, "A", 1);
        List<LetterCollection> collections = Arrays.asList(collectionA);
        when(letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID))
            .thenReturn(collections);
        
        // When
        boolean result = letterService.hasClaimableWords(TEST_USER_ID);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void getAllAvailableWords_ReturnsAllWordsWithoutClaimStatus() {
        // Given
        List<LetterWord> activeWords = Arrays.asList(testWord);
        when(letterWordRepository.findActiveWordsOrderByRewardDesc()).thenReturn(activeWords);
        
        // When
        List<LetterWordDTO> result = letterService.getAllAvailableWords();
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        LetterWordDTO dto = result.get(0);
        assertEquals(TEST_WORD_ID, dto.getId());
        assertEquals("AB", dto.getWord());
        assertFalse(dto.getCanClaim()); // Should be false without user context
    }
}