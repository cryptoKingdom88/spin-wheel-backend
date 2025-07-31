package com.casino.roulette.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class LetterWordDTOTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testDefaultConstructor() {
        LetterWordDTO dto = new LetterWordDTO();
        assertNotNull(dto);
    }

    @Test
    void testFullConstructor() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWordDTO dto = new LetterWordDTO(1L, "HAPPY", requiredLetters, 
                                             new BigDecimal("50.00"), true);
        
        assertEquals(1L, dto.getId());
        assertEquals("HAPPY", dto.getWord());
        assertEquals(requiredLetters, dto.getRequiredLetters());
        assertEquals(new BigDecimal("50.00"), dto.getRewardAmount());
        assertTrue(dto.getCanClaim());
    }

    @Test
    void testConstructorWithoutCanClaim() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("W", 1);
        requiredLetters.put("I", 1);
        requiredLetters.put("N", 1);
        
        LetterWordDTO dto = new LetterWordDTO(2L, "WIN", requiredLetters, new BigDecimal("25.00"));
        
        assertEquals(2L, dto.getId());
        assertEquals("WIN", dto.getWord());
        assertEquals(requiredLetters, dto.getRequiredLetters());
        assertEquals(new BigDecimal("25.00"), dto.getRewardAmount());
        assertFalse(dto.getCanClaim());
    }

    @Test
    void testValidLetterWordDTO() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("L", 1);
        requiredLetters.put("U", 1);
        requiredLetters.put("C", 1);
        requiredLetters.put("K", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(3L);
        dto.setWord("LUCK");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("100.00"));
        dto.setCanClaim(false);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullId() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("T", 1);
        requiredLetters.put("E", 1);
        requiredLetters.put("S", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setWord("TEST");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("10.00"));
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }

    @Test
    void testBlankWord() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(4L);
        dto.setWord("");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("10.00"));
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("word")));
    }

    @Test
    void testNullWord() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(5L);
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("10.00"));
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("word")));
    }

    @Test
    void testNullRequiredLetters() {
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(6L);
        dto.setWord("TEST");
        dto.setRewardAmount(new BigDecimal("10.00"));
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("requiredLetters")));
    }

    @Test
    void testEmptyRequiredLetters() {
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(7L);
        dto.setWord("TEST");
        dto.setRequiredLetters(new HashMap<>());
        dto.setRewardAmount(new BigDecimal("10.00"));
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("requiredLetters")));
    }

    @Test
    void testNullRewardAmount() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(8L);
        dto.setWord("TEST");
        dto.setRequiredLetters(requiredLetters);
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rewardAmount")));
    }

    @Test
    void testZeroRewardAmount() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(9L);
        dto.setWord("TEST");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(BigDecimal.ZERO);
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rewardAmount")));
    }

    @Test
    void testNegativeRewardAmount() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(10L);
        dto.setWord("TEST");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("-5.00"));
        dto.setCanClaim(true);

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rewardAmount")));
    }

    @Test
    void testNullCanClaim() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setId(11L);
        dto.setWord("TEST");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("10.00"));

        Set<ConstraintViolation<LetterWordDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("canClaim")));
    }

    @Test
    void testGetTotalLettersRequired() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setRequiredLetters(requiredLetters);
        
        assertEquals(5, dto.getTotalLettersRequired()); // 1+1+2+1 = 5
        
        // Test with null
        dto.setRequiredLetters(null);
        assertEquals(0, dto.getTotalLettersRequired());
    }

    @Test
    void testGetUniqueLettersRequired() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("H", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("P", 2);
        requiredLetters.put("Y", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setRequiredLetters(requiredLetters);
        
        assertEquals(4, dto.getUniqueLettersRequired()); // H, A, P, Y = 4 unique letters
        
        // Test with null
        dto.setRequiredLetters(null);
        assertEquals(0, dto.getUniqueLettersRequired());
    }

    @Test
    void testRequiresLetter() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("W", 1);
        requiredLetters.put("I", 1);
        requiredLetters.put("N", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setRequiredLetters(requiredLetters);
        
        assertTrue(dto.requiresLetter("W"));
        assertTrue(dto.requiresLetter("w")); // Should work with lowercase
        assertTrue(dto.requiresLetter("I"));
        assertTrue(dto.requiresLetter("N"));
        assertFalse(dto.requiresLetter("X"));
        assertFalse(dto.requiresLetter(null));
        
        // Test with null required letters
        dto.setRequiredLetters(null);
        assertFalse(dto.requiresLetter("W"));
    }

    @Test
    void testGetRequiredCount() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("B", 1);
        requiredLetters.put("O", 2);
        requiredLetters.put("K", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        dto.setRequiredLetters(requiredLetters);
        
        assertEquals(1, dto.getRequiredCount("B"));
        assertEquals(1, dto.getRequiredCount("b")); // Should work with lowercase
        assertEquals(2, dto.getRequiredCount("O"));
        assertEquals(1, dto.getRequiredCount("K"));
        assertEquals(0, dto.getRequiredCount("X"));
        assertEquals(0, dto.getRequiredCount(null));
        
        // Test with null required letters
        dto.setRequiredLetters(null);
        assertEquals(0, dto.getRequiredCount("B"));
    }

    @Test
    void testJsonSerialization() throws Exception {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("C", 1);
        requiredLetters.put("A", 1);
        requiredLetters.put("R", 1);
        
        LetterWordDTO dto = new LetterWordDTO(12L, "CAR", requiredLetters, 
                                             new BigDecimal("75.50"), true);
        
        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("\"id\":12"));
        assertTrue(json.contains("\"word\":\"CAR\""));
        assertTrue(json.contains("\"rewardAmount\":75.50"));
        assertTrue(json.contains("\"canClaim\":true"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"id\":13,\"word\":\"DOG\",\"requiredLetters\":{\"D\":1,\"O\":1,\"G\":1}," +
                     "\"rewardAmount\":30.25,\"canClaim\":false}";
        
        LetterWordDTO dto = objectMapper.readValue(json, LetterWordDTO.class);
        
        assertEquals(13L, dto.getId());
        assertEquals("DOG", dto.getWord());
        assertEquals(3, dto.getRequiredLetters().size());
        assertEquals(1, dto.getRequiredLetters().get("D"));
        assertEquals(1, dto.getRequiredLetters().get("O"));
        assertEquals(1, dto.getRequiredLetters().get("G"));
        assertEquals(new BigDecimal("30.25"), dto.getRewardAmount());
        assertFalse(dto.getCanClaim());
    }

    @Test
    void testToString() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("F", 1);
        requiredLetters.put("U", 1);
        requiredLetters.put("N", 1);
        
        LetterWordDTO dto = new LetterWordDTO(14L, "FUN", requiredLetters, 
                                             new BigDecimal("15.00"), false);
        
        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("LetterWordDTO"));
        assertTrue(toString.contains("id=14"));
        assertTrue(toString.contains("word='FUN'"));
        assertTrue(toString.contains("rewardAmount=15.00"));
        assertTrue(toString.contains("canClaim=false"));
    }

    @Test
    void testEquals() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto1 = new LetterWordDTO(15L, "A", requiredLetters, new BigDecimal("5.00"), true);
        LetterWordDTO dto2 = new LetterWordDTO(15L, "B", requiredLetters, new BigDecimal("10.00"), false);
        LetterWordDTO dto3 = new LetterWordDTO(16L, "A", requiredLetters, new BigDecimal("5.00"), true);
        
        assertEquals(dto1, dto2); // Same ID
        assertNotEquals(dto1, dto3); // Different ID
        assertNotEquals(dto1, null);
        assertNotEquals(dto1, "string");
    }

    @Test
    void testHashCode() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("A", 1);
        
        LetterWordDTO dto1 = new LetterWordDTO(17L, "TEST1", requiredLetters, new BigDecimal("5.00"), true);
        LetterWordDTO dto2 = new LetterWordDTO(17L, "TEST2", requiredLetters, new BigDecimal("10.00"), false);
        
        assertEquals(dto1.hashCode(), dto2.hashCode()); // Same ID should have same hash
    }

    @Test
    void testSettersAndGetters() {
        Map<String, Integer> requiredLetters = new HashMap<>();
        requiredLetters.put("Z", 1);
        
        LetterWordDTO dto = new LetterWordDTO();
        
        dto.setId(18L);
        dto.setWord("ZOO");
        dto.setRequiredLetters(requiredLetters);
        dto.setRewardAmount(new BigDecimal("99.99"));
        dto.setCanClaim(true);
        
        assertEquals(18L, dto.getId());
        assertEquals("ZOO", dto.getWord());
        assertEquals(requiredLetters, dto.getRequiredLetters());
        assertEquals(new BigDecimal("99.99"), dto.getRewardAmount());
        assertTrue(dto.getCanClaim());
    }
}