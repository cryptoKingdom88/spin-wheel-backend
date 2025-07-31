package com.casino.roulette.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidUser() {
        User user = new User(1L);
        user.setCashBalance(new BigDecimal("100.50"));
        user.setAvailableSpins(5);
        user.setFirstDepositBonusUsed(false);
        user.setLastDailyLogin(LocalDateTime.now());
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testUserWithNullId() {
        User user = new User();
        user.setCashBalance(new BigDecimal("100.00"));
        user.setAvailableSpins(5);
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());
        assertEquals("must not be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testUserWithNegativeCashBalance() {
        User user = new User(1L);
        user.setCashBalance(new BigDecimal("-10.00"));
        user.setAvailableSpins(5);
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());
        assertEquals("Cash balance cannot be negative", violations.iterator().next().getMessage());
    }
    
    @Test
    void testUserWithNegativeAvailableSpins() {
        User user = new User(1L);
        user.setCashBalance(new BigDecimal("100.00"));
        user.setAvailableSpins(-1);
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertEquals(1, violations.size());
        assertEquals("Available spins cannot be negative", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDefaultValues() {
        User user = new User(1L);
        
        assertEquals(BigDecimal.ZERO, user.getCashBalance());
        assertEquals(0, user.getAvailableSpins());
        assertEquals(false, user.getFirstDepositBonusUsed());
        assertNull(user.getLastDailyLogin());
    }
    
    @Test
    void testConstructorWithId() {
        Long userId = 123L;
        User user = new User(userId);
        
        assertEquals(userId, user.getId());
        assertEquals(BigDecimal.ZERO, user.getCashBalance());
        assertEquals(0, user.getAvailableSpins());
        assertEquals(false, user.getFirstDepositBonusUsed());
    }
    
    @Test
    void testEqualsAndHashCode() {
        User user1 = new User(1L);
        User user2 = new User(1L);
        User user3 = new User(2L);
        
        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
    
    @Test
    void testToString() {
        User user = new User(1L);
        user.setCashBalance(new BigDecimal("50.25"));
        user.setAvailableSpins(3);
        user.setFirstDepositBonusUsed(true);
        
        String toString = user.toString();
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("cashBalance=50.25"));
        assertTrue(toString.contains("availableSpins=3"));
        assertTrue(toString.contains("firstDepositBonusUsed=true"));
    }
    
    @Test
    void testSettersAndGetters() {
        User user = new User();
        Long id = 1L;
        BigDecimal balance = new BigDecimal("75.50");
        Integer spins = 10;
        Boolean bonusUsed = true;
        LocalDateTime loginTime = LocalDateTime.now();
        
        user.setId(id);
        user.setCashBalance(balance);
        user.setAvailableSpins(spins);
        user.setFirstDepositBonusUsed(bonusUsed);
        user.setLastDailyLogin(loginTime);
        
        assertEquals(id, user.getId());
        assertEquals(balance, user.getCashBalance());
        assertEquals(spins, user.getAvailableSpins());
        assertEquals(bonusUsed, user.getFirstDepositBonusUsed());
        assertEquals(loginTime, user.getLastDailyLogin());
    }
}