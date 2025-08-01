package com.casino.roulette.service;

import com.casino.roulette.dto.LetterWordDTO;
import com.casino.roulette.entity.LetterCollection;
import com.casino.roulette.entity.LetterWord;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.LetterCollectionRepository;
import com.casino.roulette.repository.LetterWordRepository;
import com.casino.roulette.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LetterWordClaimTest {
    
    @Autowired
    private LetterService letterService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LetterWordRepository letterWordRepository;
    
    @Autowired
    private LetterCollectionRepository letterCollectionRepository;
    
    private User testUser;
    private LetterWord happyWord;
    private final Long TEST_USER_ID = 999L;
    
    @BeforeEach
    void setUp() {
        // Clean up
        letterCollectionRepository.deleteAll();
        letterWordRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test user
        testUser = new User(TEST_USER_ID);
        testUser.setCashBalance(BigDecimal.ZERO);
        userRepository.save(testUser);
        
        // Create HAPPY word requirement: H:1, A:1, P:2, Y:1
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        happyWord = new LetterWord("HAPPY", new BigDecimal("25.00"), true);
        happyWord.setRequiredLettersMap(requiredLetters);
        letterWordRepository.save(happyWord);
    }
    
    @Test
    void testCanClaimWithExactLetters() {
        // Given: User has exactly the required letters H:1, A:1, P:2, Y:1
        letterCollectionRepository.saveAll(Arrays.asList(
            new LetterCollection(TEST_USER_ID, "H", 1),
            new LetterCollection(TEST_USER_ID, "A", 1),
            new LetterCollection(TEST_USER_ID, "P", 2), // Exactly 2 P's
            new LetterCollection(TEST_USER_ID, "Y", 1)
        ));
        
        // When: Get available words
        List<LetterWordDTO> words = letterService.getAvailableWords(TEST_USER_ID);
        
        // Then: Should be able to claim HAPPY
        assertEquals(1, words.size());
        LetterWordDTO happyDTO = words.get(0);
        assertEquals("HAPPY", happyDTO.getWord());
        assertTrue(happyDTO.getCanClaim(), "Should be able to claim HAPPY with H:1, A:1, P:2, Y:1");
        
        // Debug: Print actual vs required
        System.out.println("Required letters: " + happyDTO.getRequiredLetters());
        System.out.println("User collections:");
        letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID)
            .forEach(c -> System.out.println("  " + c.getLetter() + ": " + c.getCount()));
    }
    
    @Test
    void testCannotClaimWithInsufficientLetters() {
        // Given: User has H:1, A:2, P:1, Y:1 (missing 1 P)
        letterCollectionRepository.saveAll(Arrays.asList(
            new LetterCollection(TEST_USER_ID, "H", 1),
            new LetterCollection(TEST_USER_ID, "A", 2),
            new LetterCollection(TEST_USER_ID, "P", 1), // Only 1 P, need 2
            new LetterCollection(TEST_USER_ID, "Y", 1)
        ));
        
        // When: Get available words
        List<LetterWordDTO> words = letterService.getAvailableWords(TEST_USER_ID);
        
        // Then: Should NOT be able to claim HAPPY
        assertEquals(1, words.size());
        LetterWordDTO happyDTO = words.get(0);
        assertEquals("HAPPY", happyDTO.getWord());
        assertFalse(happyDTO.getCanClaim(), "Should NOT be able to claim HAPPY with insufficient P letters");
        
        // Debug: Print actual vs required
        System.out.println("Required letters: " + happyDTO.getRequiredLetters());
        System.out.println("User collections:");
        letterCollectionRepository.findByUserIdWithPositiveCount(TEST_USER_ID)
            .forEach(c -> System.out.println("  " + c.getLetter() + ": " + c.getCount()));
    }
    
    @Test
    void testCanClaimWithExtraLetters() {
        // Given: User has more than required H:2, A:3, P:3, Y:2
        letterCollectionRepository.saveAll(Arrays.asList(
            new LetterCollection(TEST_USER_ID, "H", 2),
            new LetterCollection(TEST_USER_ID, "A", 3),
            new LetterCollection(TEST_USER_ID, "P", 3), // More than required
            new LetterCollection(TEST_USER_ID, "Y", 2)
        ));
        
        // When: Get available words
        List<LetterWordDTO> words = letterService.getAvailableWords(TEST_USER_ID);
        
        // Then: Should be able to claim HAPPY
        assertEquals(1, words.size());
        LetterWordDTO happyDTO = words.get(0);
        assertEquals("HAPPY", happyDTO.getWord());
        assertTrue(happyDTO.getCanClaim(), "Should be able to claim HAPPY with extra letters");
    }
}