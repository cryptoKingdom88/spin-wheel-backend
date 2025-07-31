package com.casino.roulette.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LetterCollectionTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidLetterCollection() {
        LetterCollection collection = new LetterCollection(1L, "A", 5);
        
        Set<ConstraintViolation<LetterCollection>> violations = validator.validate(collection);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testLetterCollectionWithNullUserId() {
        LetterCollection collection = new LetterCollection();
        collection.setUserId(null);
        collection.setLetter("A");
        collection.setCount(5);
        
        Set<ConstraintViolation<LetterCollection>> violations = validator.validate(collection);
        assertEquals(1, violations.size());
        assertEquals("User ID cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testLetterCollectionWithBlankLetter() {
        LetterCollection collection = new LetterCollection();
        collection.setUserId(1L);
        collection.setLetter("");
        collection.setCount(5);
        
        Set<ConstraintViolation<LetterCollection>> violations = validator.validate(collection);
        assertEquals(2, violations.size()); // Both @NotBlank and @Size violations
        
        boolean hasNotBlankViolation = violations.stream()
            .anyMatch(v -> "Letter cannot be blank".equals(v.getMessage()));
        boolean hasSizeViolation = violations.stream()
            .anyMatch(v -> "Letter must be exactly one character".equals(v.getMessage()));
        
        assertTrue(hasNotBlankViolation);
        assertTrue(hasSizeViolation);
    }
    
    @Test
    void testLetterCollectionWithMultiCharacterLetter() {
        LetterCollection collection = new LetterCollection();
        collection.setUserId(1L);
        collection.setLetter("AB");
        collection.setCount(5);
        
        Set<ConstraintViolation<LetterCollection>> violations = validator.validate(collection);
        assertEquals(1, violations.size());
        assertEquals("Letter must be exactly one character", violations.iterator().next().getMessage());
    }
    
    @Test
    void testLetterCollectionWithNegativeCount() {
        LetterCollection collection = new LetterCollection(1L, "A", -1);
        
        Set<ConstraintViolation<LetterCollection>> violations = validator.validate(collection);
        assertEquals(1, violations.size());
        assertEquals("Count cannot be negative", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDefaultValues() {
        LetterCollection collection = new LetterCollection();
        assertEquals(0, collection.getCount());
    }
    
    @Test
    void testConstructorWithUserIdAndLetter() {
        Long userId = 123L;
        String letter = "b";
        
        LetterCollection collection = new LetterCollection(userId, letter);
        
        assertEquals(userId, collection.getUserId());
        assertEquals("B", collection.getLetter()); // Should be uppercase
        assertEquals(0, collection.getCount());
    }
    
    @Test
    void testConstructorWithCount() {
        Long userId = 123L;
        String letter = "c";
        Integer count = 10;
        
        LetterCollection collection = new LetterCollection(userId, letter, count);
        
        assertEquals(userId, collection.getUserId());
        assertEquals("C", collection.getLetter()); // Should be uppercase
        assertEquals(count, collection.getCount());
    }
    
    @Test
    void testLetterUppercaseConversion() {
        LetterCollection collection = new LetterCollection();
        
        collection.setLetter("a");
        assertEquals("A", collection.getLetter());
        
        collection.setLetter("z");
        assertEquals("Z", collection.getLetter());
        
        collection.setLetter("A");
        assertEquals("A", collection.getLetter());
    }
    
    @Test
    void testLetterNullHandling() {
        LetterCollection collection = new LetterCollection();
        collection.setLetter(null);
        assertNull(collection.getLetter());
        
        LetterCollection collection2 = new LetterCollection(1L, null);
        assertNull(collection2.getLetter());
    }
    
    @Test
    void testIncrementCount() {
        LetterCollection collection = new LetterCollection(1L, "A", 5);
        
        collection.incrementCount();
        assertEquals(6, collection.getCount());
        
        collection.incrementCount();
        assertEquals(7, collection.getCount());
    }
    
    @Test
    void testIncrementCountWithAmount() {
        LetterCollection collection = new LetterCollection(1L, "A", 5);
        
        collection.incrementCount(3);
        assertEquals(8, collection.getCount());
        
        collection.incrementCount(0);
        assertEquals(8, collection.getCount()); // Should not change
        
        collection.incrementCount(-1);
        assertEquals(8, collection.getCount()); // Should not change for negative
        
        collection.incrementCount(null);
        assertEquals(8, collection.getCount()); // Should not change for null
    }
    
    @Test
    void testDecrementCount() {
        LetterCollection collection = new LetterCollection(1L, "A", 10);
        
        collection.decrementCount(3);
        assertEquals(7, collection.getCount());
        
        collection.decrementCount(10);
        assertEquals(0, collection.getCount()); // Should not go below 0
        
        collection.setCount(5);
        collection.decrementCount(0);
        assertEquals(5, collection.getCount()); // Should not change
        
        collection.decrementCount(-1);
        assertEquals(5, collection.getCount()); // Should not change for negative
        
        collection.decrementCount(null);
        assertEquals(5, collection.getCount()); // Should not change for null
    }
    
    @Test
    void testHasAtLeast() {
        LetterCollection collection = new LetterCollection(1L, "A", 5);
        
        assertTrue(collection.hasAtLeast(3));
        assertTrue(collection.hasAtLeast(5));
        assertFalse(collection.hasAtLeast(6));
        assertFalse(collection.hasAtLeast(10));
        
        // Test with null
        assertFalse(collection.hasAtLeast(null));
        
        // Test with zero count
        collection.setCount(0);
        assertFalse(collection.hasAtLeast(1));
        assertTrue(collection.hasAtLeast(0));
    }
    
    @Test
    void testIsEmpty() {
        LetterCollection collection = new LetterCollection(1L, "A", 0);
        assertTrue(collection.isEmpty());
        
        collection.setCount(1);
        assertFalse(collection.isEmpty());
        
        collection.setCount(10);
        assertFalse(collection.isEmpty());
    }
    
    @Test
    void testSettersAndGetters() {
        LetterCollection collection = new LetterCollection();
        Long id = 1L;
        Long userId = 123L;
        String letter = "X";
        Integer count = 15;
        
        collection.setId(id);
        collection.setUserId(userId);
        collection.setLetter(letter);
        collection.setCount(count);
        
        assertEquals(id, collection.getId());
        assertEquals(userId, collection.getUserId());
        assertEquals(letter, collection.getLetter());
        assertEquals(count, collection.getCount());
    }
    
    @Test
    void testEqualsAndHashCode() {
        LetterCollection collection1 = new LetterCollection();
        collection1.setId(1L);
        
        LetterCollection collection2 = new LetterCollection();
        collection2.setId(1L);
        
        LetterCollection collection3 = new LetterCollection();
        collection3.setId(2L);
        
        assertEquals(collection1, collection2);
        assertNotEquals(collection1, collection3);
        assertEquals(collection1.hashCode(), collection2.hashCode());
    }
    
    @Test
    void testToString() {
        LetterCollection collection = new LetterCollection(123L, "A", 7);
        
        String toString = collection.toString();
        assertTrue(toString.contains("userId=123"));
        assertTrue(toString.contains("letter='A'"));
        assertTrue(toString.contains("count=7"));
    }
}