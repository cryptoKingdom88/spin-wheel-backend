package com.casino.roulette.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpinResultDTOTest {

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
        SpinResultDTO dto = new SpinResultDTO();
        assertNotNull(dto);
    }

    @Test
    void testCashWinConstructor() {
        SpinResultDTO dto = new SpinResultDTO("CASH", "10.50", new BigDecimal("10.50"), 5);
        
        assertEquals("CASH", dto.getType());
        assertEquals("10.50", dto.getValue());
        assertEquals(new BigDecimal("10.50"), dto.getCashWon());
        assertNull(dto.getLetterWon());
        assertEquals(5, dto.getRemainingSpins());
    }

    @Test
    void testLetterWinConstructor() {
        SpinResultDTO dto = new SpinResultDTO("LETTER", "A", "A", 4);
        
        assertEquals("LETTER", dto.getType());
        assertEquals("A", dto.getValue());
        assertNull(dto.getCashWon());
        assertEquals("A", dto.getLetterWon());
        assertEquals(4, dto.getRemainingSpins());
    }

    @Test
    void testFullConstructor() {
        SpinResultDTO dto = new SpinResultDTO("CASH", "5.00", new BigDecimal("5.00"), null, 3);
        
        assertEquals("CASH", dto.getType());
        assertEquals("5.00", dto.getValue());
        assertEquals(new BigDecimal("5.00"), dto.getCashWon());
        assertNull(dto.getLetterWon());
        assertEquals(3, dto.getRemainingSpins());
    }

    @Test
    void testValidCashResult() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("CASH");
        dto.setValue("25.00");
        dto.setCashWon(new BigDecimal("25.00"));
        dto.setRemainingSpins(2);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidLetterResult() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("LETTER");
        dto.setValue("B");
        dto.setLetterWon("B");
        dto.setRemainingSpins(1);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidType() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("INVALID");
        dto.setValue("test");
        dto.setRemainingSpins(0);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Type must be either CASH or LETTER")));
    }

    @Test
    void testNullType() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setValue("test");
        dto.setRemainingSpins(0);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void testNullValue() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("CASH");
        dto.setRemainingSpins(0);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("value")));
    }

    @Test
    void testNegativeCashWon() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("CASH");
        dto.setValue("10.00");
        dto.setCashWon(new BigDecimal("-5.00"));
        dto.setRemainingSpins(0);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cashWon")));
    }

    @Test
    void testNegativeRemainingSpins() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("CASH");
        dto.setValue("10.00");
        dto.setCashWon(new BigDecimal("10.00"));
        dto.setRemainingSpins(-1);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("remainingSpins")));
    }

    @Test
    void testInvalidLetterWon() {
        SpinResultDTO dto = new SpinResultDTO();
        dto.setType("LETTER");
        dto.setValue("AB");
        dto.setLetterWon("AB");
        dto.setRemainingSpins(0);

        Set<ConstraintViolation<SpinResultDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Letter must be a single uppercase letter")));
    }

    @Test
    void testJsonSerialization() throws Exception {
        SpinResultDTO dto = new SpinResultDTO("CASH", "15.75", new BigDecimal("15.75"), 3);
        
        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"CASH\""));
        assertTrue(json.contains("\"value\":\"15.75\""));
        assertTrue(json.contains("\"cashWon\":15.75"));
        assertTrue(json.contains("\"remainingSpins\":3"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"type\":\"LETTER\",\"value\":\"C\",\"letterWon\":\"C\",\"remainingSpins\":2}";
        
        SpinResultDTO dto = objectMapper.readValue(json, SpinResultDTO.class);
        
        assertEquals("LETTER", dto.getType());
        assertEquals("C", dto.getValue());
        assertEquals("C", dto.getLetterWon());
        assertEquals(2, dto.getRemainingSpins());
        assertNull(dto.getCashWon());
    }

    @Test
    void testToString() {
        SpinResultDTO dto = new SpinResultDTO("CASH", "20.00", new BigDecimal("20.00"), 1);
        
        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("SpinResultDTO"));
        assertTrue(toString.contains("type='CASH'"));
        assertTrue(toString.contains("value='20.00'"));
        assertTrue(toString.contains("cashWon=20.00"));
        assertTrue(toString.contains("remainingSpins=1"));
    }

    @Test
    void testSettersAndGetters() {
        SpinResultDTO dto = new SpinResultDTO();
        
        dto.setType("LETTER");
        dto.setValue("Z");
        dto.setLetterWon("Z");
        dto.setRemainingSpins(0);
        
        assertEquals("LETTER", dto.getType());
        assertEquals("Z", dto.getValue());
        assertEquals("Z", dto.getLetterWon());
        assertEquals(0, dto.getRemainingSpins());
        assertNull(dto.getCashWon());
    }
}