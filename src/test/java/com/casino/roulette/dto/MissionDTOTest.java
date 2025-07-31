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

class MissionDTOTest {

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
        MissionDTO dto = new MissionDTO();
        assertNotNull(dto);
    }

    @Test
    void testFullConstructor() {
        MissionDTO dto = new MissionDTO(1L, "First Deposit", "Get spins for your first deposit", 
                                       1, true, 0, 1);
        
        assertEquals(1L, dto.getId());
        assertEquals("First Deposit", dto.getName());
        assertEquals("Get spins for your first deposit", dto.getDescription());
        assertEquals(1, dto.getSpinsAvailable());
        assertTrue(dto.getCanClaim());
        assertEquals(0, dto.getClaimsUsed());
        assertEquals(1, dto.getMaxClaims());
    }

    @Test
    void testConstructorWithoutDescription() {
        MissionDTO dto = new MissionDTO(2L, "Daily Login", 1, false, 1, 1);
        
        assertEquals(2L, dto.getId());
        assertEquals("Daily Login", dto.getName());
        assertNull(dto.getDescription());
        assertEquals(1, dto.getSpinsAvailable());
        assertFalse(dto.getCanClaim());
        assertEquals(1, dto.getClaimsUsed());
        assertEquals(1, dto.getMaxClaims());
    }

    @Test
    void testValidMissionDTO() {
        MissionDTO dto = new MissionDTO();
        dto.setId(3L);
        dto.setName("Deposit $100");
        dto.setDescription("Deposit at least $100 to get spins");
        dto.setSpinsAvailable(2);
        dto.setCanClaim(true);
        dto.setClaimsUsed(5);
        dto.setMaxClaims(100);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullId() {
        MissionDTO dto = new MissionDTO();
        dto.setName("Test Mission");
        dto.setCanClaim(true);
        dto.setSpinsAvailable(1);
        dto.setClaimsUsed(0);
        dto.setMaxClaims(1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("id")));
    }

    @Test
    void testBlankName() {
        MissionDTO dto = new MissionDTO();
        dto.setId(1L);
        dto.setName("");
        dto.setCanClaim(true);
        dto.setSpinsAvailable(1);
        dto.setClaimsUsed(0);
        dto.setMaxClaims(1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void testNullName() {
        MissionDTO dto = new MissionDTO();
        dto.setId(1L);
        dto.setCanClaim(true);
        dto.setSpinsAvailable(1);
        dto.setClaimsUsed(0);
        dto.setMaxClaims(1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void testNullCanClaim() {
        MissionDTO dto = new MissionDTO();
        dto.setId(1L);
        dto.setName("Test Mission");
        dto.setSpinsAvailable(1);
        dto.setClaimsUsed(0);
        dto.setMaxClaims(1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("canClaim")));
    }

    @Test
    void testNegativeSpinsAvailable() {
        MissionDTO dto = new MissionDTO();
        dto.setId(1L);
        dto.setName("Test Mission");
        dto.setCanClaim(true);
        dto.setSpinsAvailable(-1);
        dto.setClaimsUsed(0);
        dto.setMaxClaims(1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("spinsAvailable")));
    }

    @Test
    void testNegativeClaimsUsed() {
        MissionDTO dto = new MissionDTO();
        dto.setId(1L);
        dto.setName("Test Mission");
        dto.setCanClaim(true);
        dto.setSpinsAvailable(1);
        dto.setClaimsUsed(-1);
        dto.setMaxClaims(1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("claimsUsed")));
    }

    @Test
    void testNegativeMaxClaims() {
        MissionDTO dto = new MissionDTO();
        dto.setId(1L);
        dto.setName("Test Mission");
        dto.setCanClaim(true);
        dto.setSpinsAvailable(1);
        dto.setClaimsUsed(0);
        dto.setMaxClaims(-1);

        Set<ConstraintViolation<MissionDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("maxClaims")));
    }

    @Test
    void testHasRemainingClaims() {
        MissionDTO dto = new MissionDTO();
        
        // Test with remaining claims
        dto.setClaimsUsed(3);
        dto.setMaxClaims(5);
        assertTrue(dto.hasRemainingClaims());
        
        // Test with no remaining claims
        dto.setClaimsUsed(5);
        dto.setMaxClaims(5);
        assertFalse(dto.hasRemainingClaims());
        
        // Test with exceeded claims
        dto.setClaimsUsed(6);
        dto.setMaxClaims(5);
        assertFalse(dto.hasRemainingClaims());
        
        // Test with null values
        dto.setClaimsUsed(null);
        dto.setMaxClaims(5);
        assertFalse(dto.hasRemainingClaims());
        
        dto.setClaimsUsed(3);
        dto.setMaxClaims(null);
        assertFalse(dto.hasRemainingClaims());
    }

    @Test
    void testGetRemainingClaims() {
        MissionDTO dto = new MissionDTO();
        
        // Test with remaining claims
        dto.setClaimsUsed(2);
        dto.setMaxClaims(10);
        assertEquals(8, dto.getRemainingClaims());
        
        // Test with no remaining claims
        dto.setClaimsUsed(10);
        dto.setMaxClaims(10);
        assertEquals(0, dto.getRemainingClaims());
        
        // Test with exceeded claims (should return 0)
        dto.setClaimsUsed(15);
        dto.setMaxClaims(10);
        assertEquals(0, dto.getRemainingClaims());
        
        // Test with null values
        dto.setClaimsUsed(null);
        dto.setMaxClaims(10);
        assertEquals(0, dto.getRemainingClaims());
        
        dto.setClaimsUsed(5);
        dto.setMaxClaims(null);
        assertEquals(0, dto.getRemainingClaims());
    }

    @Test
    void testJsonSerialization() throws Exception {
        MissionDTO dto = new MissionDTO(1L, "Test Mission", "Test Description", 
                                       3, true, 1, 5);
        
        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"Test Mission\""));
        assertTrue(json.contains("\"description\":\"Test Description\""));
        assertTrue(json.contains("\"spinsAvailable\":3"));
        assertTrue(json.contains("\"canClaim\":true"));
        assertTrue(json.contains("\"claimsUsed\":1"));
        assertTrue(json.contains("\"maxClaims\":5"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"id\":2,\"name\":\"Daily Bonus\",\"description\":\"Daily login bonus\"," +
                     "\"spinsAvailable\":1,\"canClaim\":false,\"claimsUsed\":1,\"maxClaims\":1}";
        
        MissionDTO dto = objectMapper.readValue(json, MissionDTO.class);
        
        assertEquals(2L, dto.getId());
        assertEquals("Daily Bonus", dto.getName());
        assertEquals("Daily login bonus", dto.getDescription());
        assertEquals(1, dto.getSpinsAvailable());
        assertFalse(dto.getCanClaim());
        assertEquals(1, dto.getClaimsUsed());
        assertEquals(1, dto.getMaxClaims());
    }

    @Test
    void testToString() {
        MissionDTO dto = new MissionDTO(1L, "Test Mission", "Test Description", 
                                       2, true, 0, 3);
        
        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("MissionDTO"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("name='Test Mission'"));
        assertTrue(toString.contains("description='Test Description'"));
        assertTrue(toString.contains("spinsAvailable=2"));
        assertTrue(toString.contains("canClaim=true"));
        assertTrue(toString.contains("claimsUsed=0"));
        assertTrue(toString.contains("maxClaims=3"));
    }

    @Test
    void testSettersAndGetters() {
        MissionDTO dto = new MissionDTO();
        
        dto.setId(5L);
        dto.setName("Setter Test");
        dto.setDescription("Testing setters");
        dto.setSpinsAvailable(4);
        dto.setCanClaim(false);
        dto.setClaimsUsed(2);
        dto.setMaxClaims(6);
        
        assertEquals(5L, dto.getId());
        assertEquals("Setter Test", dto.getName());
        assertEquals("Testing setters", dto.getDescription());
        assertEquals(4, dto.getSpinsAvailable());
        assertFalse(dto.getCanClaim());
        assertEquals(2, dto.getClaimsUsed());
        assertEquals(6, dto.getMaxClaims());
    }
}