package com.casino.roulette.entity;

import com.casino.roulette.entity.RouletteSlot.SlotType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouletteSlotTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidCashRouletteSlot() {
        RouletteSlot slot = new RouletteSlot(SlotType.CASH, "10.00", 5);
        
        Set<ConstraintViolation<RouletteSlot>> violations = validator.validate(slot);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testValidLetterRouletteSlot() {
        RouletteSlot slot = new RouletteSlot(SlotType.LETTER, "A", 3);
        
        Set<ConstraintViolation<RouletteSlot>> violations = validator.validate(slot);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testRouletteSlotWithNullSlotType() {
        RouletteSlot slot = new RouletteSlot();
        slot.setSlotType(null);
        slot.setSlotValue("10.00");
        slot.setWeight(5);
        
        Set<ConstraintViolation<RouletteSlot>> violations = validator.validate(slot);
        assertEquals(1, violations.size());
        assertEquals("Slot type cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testRouletteSlotWithBlankSlotValue() {
        RouletteSlot slot = new RouletteSlot();
        slot.setSlotType(SlotType.CASH);
        slot.setSlotValue("");
        slot.setWeight(5);
        
        Set<ConstraintViolation<RouletteSlot>> violations = validator.validate(slot);
        assertEquals(1, violations.size());
        assertEquals("Slot value cannot be blank", violations.iterator().next().getMessage());
    }
    
    @Test
    void testRouletteSlotWithNullWeight() {
        RouletteSlot slot = new RouletteSlot();
        slot.setSlotType(SlotType.CASH);
        slot.setSlotValue("10.00");
        slot.setWeight(null);
        
        Set<ConstraintViolation<RouletteSlot>> violations = validator.validate(slot);
        assertEquals(1, violations.size());
        assertEquals("Weight cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testRouletteSlotWithZeroWeight() {
        RouletteSlot slot = new RouletteSlot(SlotType.CASH, "10.00", 0);
        
        Set<ConstraintViolation<RouletteSlot>> violations = validator.validate(slot);
        assertEquals(1, violations.size());
        assertEquals("Weight must be at least 1", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDefaultValues() {
        RouletteSlot slot = new RouletteSlot();
        assertEquals(1, slot.getWeight());
        assertEquals(true, slot.getActive());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        SlotType type = SlotType.LETTER;
        String value = "B";
        Integer weight = 7;
        
        RouletteSlot slot = new RouletteSlot(type, value, weight);
        
        assertEquals(type, slot.getSlotType());
        assertEquals(value, slot.getSlotValue());
        assertEquals(weight, slot.getWeight());
        assertEquals(true, slot.getActive());
    }
    
    @Test
    void testIsCashSlot() {
        RouletteSlot cashSlot = new RouletteSlot(SlotType.CASH, "25.00", 3);
        RouletteSlot letterSlot = new RouletteSlot(SlotType.LETTER, "C", 2);
        
        assertTrue(cashSlot.isCashSlot());
        assertFalse(letterSlot.isCashSlot());
    }
    
    @Test
    void testIsLetterSlot() {
        RouletteSlot cashSlot = new RouletteSlot(SlotType.CASH, "25.00", 3);
        RouletteSlot letterSlot = new RouletteSlot(SlotType.LETTER, "C", 2);
        
        assertFalse(cashSlot.isLetterSlot());
        assertTrue(letterSlot.isLetterSlot());
    }
    
    @Test
    void testGetCashValue() {
        RouletteSlot cashSlot = new RouletteSlot(SlotType.CASH, "25.00", 3);
        RouletteSlot letterSlot = new RouletteSlot(SlotType.LETTER, "C", 2);
        
        assertEquals("25.00", cashSlot.getCashValue());
        assertNull(letterSlot.getCashValue());
    }
    
    @Test
    void testGetLetterValue() {
        RouletteSlot cashSlot = new RouletteSlot(SlotType.CASH, "25.00", 3);
        RouletteSlot letterSlot = new RouletteSlot(SlotType.LETTER, "C", 2);
        
        assertNull(cashSlot.getLetterValue());
        assertEquals("C", letterSlot.getLetterValue());
    }
    
    @Test
    void testSlotTypeEnum() {
        assertEquals("CASH", SlotType.CASH.name());
        assertEquals("LETTER", SlotType.LETTER.name());
        assertEquals(2, SlotType.values().length);
    }
    
    @Test
    void testSettersAndGetters() {
        RouletteSlot slot = new RouletteSlot();
        Long id = 1L;
        SlotType type = SlotType.CASH;
        String value = "50.00";
        Integer weight = 10;
        Boolean active = false;
        
        slot.setId(id);
        slot.setSlotType(type);
        slot.setSlotValue(value);
        slot.setWeight(weight);
        slot.setActive(active);
        
        assertEquals(id, slot.getId());
        assertEquals(type, slot.getSlotType());
        assertEquals(value, slot.getSlotValue());
        assertEquals(weight, slot.getWeight());
        assertEquals(active, slot.getActive());
    }
    
    @Test
    void testEqualsAndHashCode() {
        RouletteSlot slot1 = new RouletteSlot();
        slot1.setId(1L);
        
        RouletteSlot slot2 = new RouletteSlot();
        slot2.setId(1L);
        
        RouletteSlot slot3 = new RouletteSlot();
        slot3.setId(2L);
        
        assertEquals(slot1, slot2);
        assertNotEquals(slot1, slot3);
        assertEquals(slot1.hashCode(), slot2.hashCode());
    }
    
    @Test
    void testToString() {
        RouletteSlot slot = new RouletteSlot(SlotType.CASH, "15.50", 4);
        slot.setActive(false);
        
        String toString = slot.toString();
        assertTrue(toString.contains("slotType=CASH"));
        assertTrue(toString.contains("slotValue='15.50'"));
        assertTrue(toString.contains("weight=4"));
        assertTrue(toString.contains("active=false"));
    }
}