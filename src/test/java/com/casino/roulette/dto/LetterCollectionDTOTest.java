package com.casino.roulette.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LetterCollectionDTOTest {

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
        LetterCollectionDTO dto = new LetterCollectionDTO();
        assertNotNull(dto);
    }

    @Test
    void testConstructor() {
        LetterCollectionDTO dto = new LetterCollectionDTO("A", 5);
        
        assertEquals("A", dto.getLetter());
        assertEquals(5, dto.getCount());
    }

    @Test
    void testValidLetterCollection() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setLetter("B");
        dto.setCount(3);

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullLetter() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setCount(1);

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("letter")));
    }

    @Test
    void testInvalidLetter() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setLetter("AB");
        dto.setCount(1);

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Letter must be a single uppercase letter")));
    }

    @Test
    void testLowercaseLetter() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setLetter("a");
        dto.setCount(1);

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Letter must be a single uppercase letter")));
    }

    @Test
    void testNullCount() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setLetter("C");

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("count")));
    }

    @Test
    void testNegativeCount() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setLetter("D");
        dto.setCount(-1);

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("count")));
    }

    @Test
    void testZeroCount() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        dto.setLetter("E");
        dto.setCount(0);

        Set<ConstraintViolation<LetterCollectionDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty()); // Zero should be valid (PositiveOrZero)
    }

    @Test
    void testHasLetter() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        
        // Test with positive count
        dto.setCount(5);
        assertTrue(dto.hasLetter());
        
        // Test with zero count
        dto.setCount(0);
        assertFalse(dto.hasLetter());
        
        // Test with null count
        dto.setCount(null);
        assertFalse(dto.hasLetter());
    }

    @Test
    void testHasAtLeast() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        
        // Test with sufficient count
        dto.setCount(10);
        assertTrue(dto.hasAtLeast(5));
        assertTrue(dto.hasAtLeast(10));
        assertFalse(dto.hasAtLeast(15));
        
        // Test with zero count
        dto.setCount(0);
        assertFalse(dto.hasAtLeast(1));
        assertTrue(dto.hasAtLeast(0));
        
        // Test with null count
        dto.setCount(null);
        assertFalse(dto.hasAtLeast(1));
        assertFalse(dto.hasAtLeast(0));
    }

    @Test
    void testJsonSerialization() throws Exception {
        LetterCollectionDTO dto = new LetterCollectionDTO("F", 7);
        
        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("\"letter\":\"F\""));
        assertTrue(json.contains("\"count\":7"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"letter\":\"G\",\"count\":12}";
        
        LetterCollectionDTO dto = objectMapper.readValue(json, LetterCollectionDTO.class);
        
        assertEquals("G", dto.getLetter());
        assertEquals(12, dto.getCount());
    }

    @Test
    void testToString() {
        LetterCollectionDTO dto = new LetterCollectionDTO("H", 3);
        
        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("LetterCollectionDTO"));
        assertTrue(toString.contains("letter='H'"));
        assertTrue(toString.contains("count=3"));
    }

    @Test
    void testEquals() {
        LetterCollectionDTO dto1 = new LetterCollectionDTO("I", 4);
        LetterCollectionDTO dto2 = new LetterCollectionDTO("I", 4);
        LetterCollectionDTO dto3 = new LetterCollectionDTO("J", 4);
        LetterCollectionDTO dto4 = new LetterCollectionDTO("I", 5);
        
        assertEquals(dto1, dto2);
        assertNotEquals(dto1, dto3);
        assertNotEquals(dto1, dto4);
        assertNotEquals(dto1, null);
        assertNotEquals(dto1, "string");
    }

    @Test
    void testHashCode() {
        LetterCollectionDTO dto1 = new LetterCollectionDTO("K", 6);
        LetterCollectionDTO dto2 = new LetterCollectionDTO("K", 6);
        
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testSettersAndGetters() {
        LetterCollectionDTO dto = new LetterCollectionDTO();
        
        dto.setLetter("L");
        dto.setCount(8);
        
        assertEquals("L", dto.getLetter());
        assertEquals(8, dto.getCount());
    }
}