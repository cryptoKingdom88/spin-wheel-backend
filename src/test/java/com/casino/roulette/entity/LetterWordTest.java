package com.casino.roulette.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LetterWordTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidLetterWord() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWord word = new LetterWord("HAPPY", requiredLetters, new BigDecimal("25.00"));
        
        Set<ConstraintViolation<LetterWord>> violations = validator.validate(word);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testLetterWordWithBlankWord() {
        LetterWord word = new LetterWord();
        word.setWord("");
        word.setRequiredLetters("{\"A\":1}");
        word.setRewardAmount(new BigDecimal("10.00"));
        
        Set<ConstraintViolation<LetterWord>> violations = validator.validate(word);
        assertEquals(1, violations.size());
        assertEquals("Word cannot be blank", violations.iterator().next().getMessage());
    }
    
    @Test
    void testLetterWordWithBlankRequiredLetters() {
        LetterWord word = new LetterWord();
        word.setWord("TEST");
        word.setRequiredLetters("");
        word.setRewardAmount(new BigDecimal("10.00"));
        
        Set<ConstraintViolation<LetterWord>> violations = validator.validate(word);
        assertEquals(1, violations.size());
        assertEquals("Required letters cannot be blank", violations.iterator().next().getMessage());
    }
    
    @Test
    void testLetterWordWithNullRewardAmount() {
        LetterWord word = new LetterWord();
        word.setWord("TEST");
        word.setRequiredLetters("{\"T\":1,\"E\":1,\"S\":1}");
        word.setRewardAmount(null);
        
        Set<ConstraintViolation<LetterWord>> violations = validator.validate(word);
        assertEquals(1, violations.size());
        assertEquals("Reward amount cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testLetterWordWithZeroRewardAmount() {
        LetterWord word = new LetterWord();
        word.setWord("TEST");
        word.setRequiredLetters("{\"T\":1,\"E\":1,\"S\":1}");
        word.setRewardAmount(BigDecimal.ZERO);
        
        Set<ConstraintViolation<LetterWord>> violations = validator.validate(word);
        assertEquals(1, violations.size());
        assertEquals("Reward amount must be greater than 0", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDefaultValues() {
        LetterWord word = new LetterWord();
        assertEquals(true, word.getActive());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        String wordText = "hello";
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("E", 1);
        requiredLetters.put("L", 2);
        requiredLetters.put("O", 1);
        BigDecimal reward = new BigDecimal("15.50");
        
        LetterWord word = new LetterWord(wordText, requiredLetters, reward);
        
        assertEquals("HELLO", word.getWord()); // Should be uppercase
        assertEquals(reward, word.getRewardAmount());
        assertEquals(true, word.getActive());
        
        Map<String, Integer> retrievedLetters = word.getRequiredLettersMap();
        assertEquals(4, retrievedLetters.size());
        assertEquals(1, retrievedLetters.get("H"));
        assertEquals(1, retrievedLetters.get("E"));
        assertEquals(2, retrievedLetters.get("L"));
        assertEquals(1, retrievedLetters.get("O"));
    }
    
    @Test
    void testWordUppercaseConversion() {
        LetterWord word = new LetterWord();
        
        word.setWord("test");
        assertEquals("TEST", word.getWord());
        
        word.setWord("MiXeD");
        assertEquals("MIXED", word.getWord());
        
        word.setWord("ALREADY");
        assertEquals("ALREADY", word.getWord());
    }
    
    @Test
    void testWordNullHandling() {
        LetterWord word = new LetterWord();
        word.setWord(null);
        assertNull(word.getWord());
        
        LetterWord word2 = new LetterWord(null, new HashMap<>(), new BigDecimal("10.00"));
        assertNull(word2.getWord());
    }
    
    @Test
    void testRequiredLettersMapHandling() {
        LetterWord word = new LetterWord();
        
        // Test setting and getting map
        Map<String, Integer> letters = new HashMap<>();
        letters.put("A", 2);
        letters.put("B", 1);
        letters.put("C", 3);
        
        word.setRequiredLettersMap(letters);
        Map<String, Integer> retrieved = word.getRequiredLettersMap();
        
        assertEquals(3, retrieved.size());
        assertEquals(2, retrieved.get("A"));
        assertEquals(1, retrieved.get("B"));
        assertEquals(3, retrieved.get("C"));
    }
    
    @Test
    void testRequiredLettersMapWithNull() {
        LetterWord word = new LetterWord();
        word.setRequiredLettersMap(null);
        
        Map<String, Integer> retrieved = word.getRequiredLettersMap();
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
        assertEquals("{}", word.getRequiredLetters());
    }
    
    @Test
    void testRequiredLettersMapWithEmptyString() {
        LetterWord word = new LetterWord();
        word.setRequiredLetters("");
        
        Map<String, Integer> retrieved = word.getRequiredLettersMap();
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }
    
    @Test
    void testRequiredLettersMapWithInvalidJson() {
        LetterWord word = new LetterWord();
        word.setRequiredLetters("invalid json");
        
        Map<String, Integer> retrieved = word.getRequiredLettersMap();
        assertNotNull(retrieved);
        assertTrue(retrieved.isEmpty());
    }
    
    @Test
    void testCanClaimWith() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWord word = new LetterWord("HAPPY", requiredLetters, new BigDecimal("25.00"));
        
        // Test with sufficient letters
        Map<String, Integer> userLetters = new HashMap<>();
        userLetters.put("H", 1);
        userLetters.put("A", 2);
        userLetters.put("P", 3);
        userLetters.put("Y", 1);
        
        assertTrue(word.canClaimWith(userLetters));
        
        // Test with exact letters
        userLetters.put("A", 1);
        userLetters.put("P", 2);
        assertTrue(word.canClaimWith(userLetters));
        
        // Test with insufficient letters
        userLetters.put("P", 1);
        assertFalse(word.canClaimWith(userLetters));
        
        // Test with missing letter
        userLetters.remove("Y");
        assertFalse(word.canClaimWith(userLetters));
        
        // Test with null user letters
        assertFalse(word.canClaimWith(null));
    }
    
    @Test
    void testCanClaimWithEmptyRequiredLetters() {
        LetterWord word = new LetterWord();
        word.setWord("TEST");
        word.setRequiredLettersMap(new HashMap<>());
        word.setRewardAmount(new BigDecimal("10.00"));
        
        Map<String, Integer> userLetters = new HashMap<>();
        userLetters.put("A", 5);
        
        assertTrue(word.canClaimWith(userLetters)); // No letters required
        assertTrue(word.canClaimWith(new HashMap<>())); // Empty user letters
    }
    
    @Test
    void testGetTotalLettersRequired() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWord word = new LetterWord("HAPPY", requiredLetters, new BigDecimal("25.00"));
        
        assertEquals(5, word.getTotalLettersRequired()); // 1+1+2+1 = 5
    }
    
    @Test
    void testGetUniqueLettersRequired() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWord word = new LetterWord("HAPPY", requiredLetters, new BigDecimal("25.00"));
        
        assertEquals(4, word.getUniqueLettersRequired()); // H, A, P, Y = 4 unique letters
    }
    
    @Test
    void testSettersAndGetters() {
        LetterWord word = new LetterWord();
        Long id = 1L;
        String wordText = "TEST";
        String requiredLetters = "{\"T\":1,\"E\":1,\"S\":1}";
        BigDecimal reward = new BigDecimal("20.00");
        Boolean active = false;
        
        word.setId(id);
        word.setWord(wordText);
        word.setRequiredLetters(requiredLetters);
        word.setRewardAmount(reward);
        word.setActive(active);
        
        assertEquals(id, word.getId());
        assertEquals(wordText, word.getWord());
        assertEquals(requiredLetters, word.getRequiredLetters());
        assertEquals(reward, word.getRewardAmount());
        assertEquals(active, word.getActive());
    }
    
    @Test
    void testEqualsAndHashCode() {
        LetterWord word1 = new LetterWord();
        word1.setId(1L);
        
        LetterWord word2 = new LetterWord();
        word2.setId(1L);
        
        LetterWord word3 = new LetterWord();
        word3.setId(2L);
        
        assertEquals(word1, word2);
        assertNotEquals(word1, word3);
        assertEquals(word1.hashCode(), word2.hashCode());
    }
    
    @Test
    void testToString() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("T", 1);
        requiredLetters.put("E", 1);
        requiredLetters.put("S", 2);
        
        LetterWord word = new LetterWord("TEST", requiredLetters, new BigDecimal("15.00"));
        word.setActive(false);
        
        String toString = word.toString();
        assertTrue(toString.contains("word='TEST'"));
        assertTrue(toString.contains("rewardAmount=15.00"));
        assertTrue(toString.contains("active=false"));
    }
}