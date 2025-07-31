package com.casino.roulette.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DepositMissionTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidDepositMission() {
        DepositMission mission = new DepositMission(
            "First Deposit Bonus",
            new BigDecimal("50.00"),
            1,
            50
        );
        mission.setMaxAmount(new BigDecimal("99.99"));
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testDepositMissionWithBlankName() {
        DepositMission mission = new DepositMission(
            "",
            new BigDecimal("50.00"),
            1,
            50
        );
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Mission name cannot be blank", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDepositMissionWithNullMinAmount() {
        DepositMission mission = new DepositMission();
        mission.setName("Test Mission");
        mission.setMinAmount(null);
        mission.setSpinsGranted(1);
        mission.setMaxClaims(50);
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Minimum amount cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDepositMissionWithZeroMinAmount() {
        DepositMission mission = new DepositMission(
            "Test Mission",
            BigDecimal.ZERO,
            1,
            50
        );
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Minimum amount must be greater than 0", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDepositMissionWithNullSpinsGranted() {
        DepositMission mission = new DepositMission();
        mission.setName("Test Mission");
        mission.setMinAmount(new BigDecimal("50.00"));
        mission.setSpinsGranted(null);
        mission.setMaxClaims(50);
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Spins granted cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDepositMissionWithZeroSpinsGranted() {
        DepositMission mission = new DepositMission(
            "Test Mission",
            new BigDecimal("50.00"),
            0,
            50
        );
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Spins granted must be at least 1", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDepositMissionWithNullMaxClaims() {
        DepositMission mission = new DepositMission();
        mission.setName("Test Mission");
        mission.setMinAmount(new BigDecimal("50.00"));
        mission.setSpinsGranted(1);
        mission.setMaxClaims(null);
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Max claims cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDepositMissionWithZeroMaxClaims() {
        DepositMission mission = new DepositMission(
            "Test Mission",
            new BigDecimal("50.00"),
            1,
            0
        );
        
        Set<ConstraintViolation<DepositMission>> violations = validator.validate(mission);
        assertEquals(1, violations.size());
        assertEquals("Max claims must be at least 1", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDefaultValues() {
        DepositMission mission = new DepositMission();
        assertEquals(true, mission.getActive());
        assertNull(mission.getMaxAmount());
    }
    
    @Test
    void testIsAmountInRange() {
        DepositMission mission = new DepositMission(
            "Test Mission",
            new BigDecimal("50.00"),
            1,
            50
        );
        mission.setMaxAmount(new BigDecimal("99.99"));
        
        // Test amount within range
        assertTrue(mission.isAmountInRange(new BigDecimal("75.00")));
        
        // Test amount at minimum boundary
        assertTrue(mission.isAmountInRange(new BigDecimal("50.00")));
        
        // Test amount at maximum boundary
        assertTrue(mission.isAmountInRange(new BigDecimal("99.99")));
        
        // Test amount below minimum
        assertFalse(mission.isAmountInRange(new BigDecimal("49.99")));
        
        // Test amount above maximum
        assertFalse(mission.isAmountInRange(new BigDecimal("100.00")));
        
        // Test null amount
        assertFalse(mission.isAmountInRange(null));
    }
    
    @Test
    void testIsAmountInRangeWithNoMaxAmount() {
        DepositMission mission = new DepositMission(
            "Test Mission",
            new BigDecimal("50.00"),
            1,
            50
        );
        // maxAmount is null by default
        
        // Test amount above minimum with no upper limit
        assertTrue(mission.isAmountInRange(new BigDecimal("1000.00")));
        
        // Test amount at minimum
        assertTrue(mission.isAmountInRange(new BigDecimal("50.00")));
        
        // Test amount below minimum
        assertFalse(mission.isAmountInRange(new BigDecimal("49.99")));
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        String name = "Test Mission";
        BigDecimal minAmount = new BigDecimal("100.00");
        Integer spinsGranted = 2;
        Integer maxClaims = 100;
        
        DepositMission mission = new DepositMission(name, minAmount, spinsGranted, maxClaims);
        
        assertEquals(name, mission.getName());
        assertEquals(minAmount, mission.getMinAmount());
        assertEquals(spinsGranted, mission.getSpinsGranted());
        assertEquals(maxClaims, mission.getMaxClaims());
        assertEquals(true, mission.getActive());
        assertNull(mission.getMaxAmount());
    }
    
    @Test
    void testEqualsAndHashCode() {
        DepositMission mission1 = new DepositMission();
        mission1.setId(1L);
        
        DepositMission mission2 = new DepositMission();
        mission2.setId(1L);
        
        DepositMission mission3 = new DepositMission();
        mission3.setId(2L);
        
        assertEquals(mission1, mission2);
        assertNotEquals(mission1, mission3);
        assertEquals(mission1.hashCode(), mission2.hashCode());
    }
    
    @Test
    void testToString() {
        DepositMission mission = new DepositMission(
            "Test Mission",
            new BigDecimal("50.00"),
            1,
            50
        );
        mission.setMaxAmount(new BigDecimal("99.99"));
        
        String toString = mission.toString();
        assertTrue(toString.contains("name='Test Mission'"));
        assertTrue(toString.contains("minAmount=50.00"));
        assertTrue(toString.contains("maxAmount=99.99"));
        assertTrue(toString.contains("spinsGranted=1"));
        assertTrue(toString.contains("maxClaims=50"));
        assertTrue(toString.contains("active=true"));
    }
}