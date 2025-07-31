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

class TransactionLogTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidTransactionLog() {
        TransactionLog log = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("100.00"), "User deposit");
        
        Set<ConstraintViolation<TransactionLog>> violations = validator.validate(log);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testTransactionLogWithNullUserId() {
        TransactionLog log = new TransactionLog();
        log.setUserId(null);
        log.setTransactionType(TransactionLog.TYPE_DEPOSIT);
        log.setDescription("Test");
        
        Set<ConstraintViolation<TransactionLog>> violations = validator.validate(log);
        assertEquals(1, violations.size());
        assertEquals("User ID cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testTransactionLogWithBlankTransactionType() {
        TransactionLog log = new TransactionLog();
        log.setUserId(1L);
        log.setTransactionType("");
        log.setDescription("Test");
        
        Set<ConstraintViolation<TransactionLog>> violations = validator.validate(log);
        assertEquals(1, violations.size());
        assertEquals("Transaction type cannot be blank", violations.iterator().next().getMessage());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        Long userId = 123L;
        String type = TransactionLog.TYPE_ROULETTE_WIN;
        String description = "Test description";
        
        TransactionLog log = new TransactionLog(userId, type, description);
        
        assertEquals(userId, log.getUserId());
        assertEquals(type, log.getTransactionType());
        assertEquals(description, log.getDescription());
        assertNull(log.getAmount());
    }
    
    @Test
    void testConstructorWithAmount() {
        Long userId = 123L;
        String type = TransactionLog.TYPE_DEPOSIT;
        BigDecimal amount = new BigDecimal("50.00");
        String description = "Test deposit";
        
        TransactionLog log = new TransactionLog(userId, type, amount, description);
        
        assertEquals(userId, log.getUserId());
        assertEquals(type, log.getTransactionType());
        assertEquals(amount, log.getAmount());
        assertEquals(description, log.getDescription());
    }
    
    @Test
    void testHasCashAmount() {
        TransactionLog log1 = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("100.00"), "Deposit");
        TransactionLog log2 = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            BigDecimal.ZERO, "Zero amount");
        TransactionLog log3 = new TransactionLog(1L, TransactionLog.TYPE_DAILY_LOGIN_SPIN, "Login spin");
        
        assertTrue(log1.hasCashAmount());
        assertFalse(log2.hasCashAmount()); // Zero amount
        assertFalse(log3.hasCashAmount()); // Null amount
    }
    
    @Test
    void testIsCashCredit() {
        TransactionLog creditLog = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("100.00"), "Deposit");
        TransactionLog debitLog = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("-50.00"), "Withdrawal");
        TransactionLog zeroLog = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            BigDecimal.ZERO, "Zero");
        TransactionLog nullLog = new TransactionLog(1L, TransactionLog.TYPE_DAILY_LOGIN_SPIN, "Login");
        
        assertTrue(creditLog.isCashCredit());
        assertFalse(debitLog.isCashCredit());
        assertFalse(zeroLog.isCashCredit());
        assertFalse(nullLog.isCashCredit());
    }
    
    @Test
    void testIsCashDebit() {
        TransactionLog creditLog = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("100.00"), "Deposit");
        TransactionLog debitLog = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("-50.00"), "Withdrawal");
        TransactionLog zeroLog = new TransactionLog(1L, TransactionLog.TYPE_DEPOSIT, 
            BigDecimal.ZERO, "Zero");
        TransactionLog nullLog = new TransactionLog(1L, TransactionLog.TYPE_DAILY_LOGIN_SPIN, "Login");
        
        assertFalse(creditLog.isCashDebit());
        assertTrue(debitLog.isCashDebit());
        assertFalse(zeroLog.isCashDebit());
        assertFalse(nullLog.isCashDebit());
    }
    
    @Test
    void testIsOfType() {
        TransactionLog log = new TransactionLog(1L, TransactionLog.TYPE_ROULETTE_WIN, 
            new BigDecimal("25.00"), "Win");
        
        assertTrue(log.isOfType(TransactionLog.TYPE_ROULETTE_WIN));
        assertFalse(log.isOfType(TransactionLog.TYPE_DEPOSIT));
        assertFalse(log.isOfType(null));
        
        // Test with null transaction type
        log.setTransactionType(null);
        assertFalse(log.isOfType(TransactionLog.TYPE_ROULETTE_WIN));
    }
    
    @Test
    void testCreateDepositLog() {
        Long userId = 123L;
        BigDecimal amount = new BigDecimal("75.50");
        
        TransactionLog log = TransactionLog.createDepositLog(userId, amount);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_DEPOSIT, log.getTransactionType());
        assertEquals(amount, log.getAmount());
        assertTrue(log.getDescription().contains("$75.50"));
        assertTrue(log.getDescription().contains("deposit"));
    }
    
    @Test
    void testCreateRouletteWinLog() {
        Long userId = 123L;
        BigDecimal amount = new BigDecimal("10.00");
        
        TransactionLog log = TransactionLog.createRouletteWinLog(userId, amount);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_ROULETTE_WIN, log.getTransactionType());
        assertEquals(amount, log.getAmount());
        assertTrue(log.getDescription().contains("$10.00"));
        assertTrue(log.getDescription().contains("Roulette"));
    }
    
    @Test
    void testCreateLetterBonusLog() {
        Long userId = 123L;
        BigDecimal amount = new BigDecimal("25.00");
        String word = "HAPPY";
        
        TransactionLog log = TransactionLog.createLetterBonusLog(userId, amount, word);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_LETTER_BONUS, log.getTransactionType());
        assertEquals(amount, log.getAmount());
        assertTrue(log.getDescription().contains("$25.00"));
        assertTrue(log.getDescription().contains("HAPPY"));
        assertTrue(log.getDescription().contains("Letter collection"));
    }
    
    @Test
    void testCreateDailyLoginSpinLog() {
        Long userId = 123L;
        
        TransactionLog log = TransactionLog.createDailyLoginSpinLog(userId);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_DAILY_LOGIN_SPIN, log.getTransactionType());
        assertNull(log.getAmount());
        assertTrue(log.getDescription().contains("Daily login"));
    }
    
    @Test
    void testCreateFirstDepositSpinLog() {
        Long userId = 123L;
        
        TransactionLog log = TransactionLog.createFirstDepositSpinLog(userId);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_FIRST_DEPOSIT_SPIN, log.getTransactionType());
        assertNull(log.getAmount());
        assertTrue(log.getDescription().contains("First deposit"));
    }
    
    @Test
    void testCreateDepositMissionSpinLog() {
        Long userId = 123L;
        String missionName = "High Roller";
        Integer spins = 5;
        
        TransactionLog log = TransactionLog.createDepositMissionSpinLog(userId, missionName, spins);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_DEPOSIT_MISSION_SPIN, log.getTransactionType());
        assertNull(log.getAmount());
        assertTrue(log.getDescription().contains("5 spin(s)"));
        assertTrue(log.getDescription().contains("High Roller"));
    }
    
    @Test
    void testCreateSpinConsumedLog() {
        Long userId = 123L;
        
        TransactionLog log = TransactionLog.createSpinConsumedLog(userId);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_SPIN_CONSUMED, log.getTransactionType());
        assertNull(log.getAmount());
        assertTrue(log.getDescription().contains("consumed"));
    }
    
    @Test
    void testCreateLetterCollectedLog() {
        Long userId = 123L;
        String letter = "A";
        
        TransactionLog log = TransactionLog.createLetterCollectedLog(userId, letter);
        
        assertEquals(userId, log.getUserId());
        assertEquals(TransactionLog.TYPE_LETTER_COLLECTED, log.getTransactionType());
        assertNull(log.getAmount());
        assertTrue(log.getDescription().contains("'A'"));
        assertTrue(log.getDescription().contains("collected"));
    }
    
    @Test
    void testTransactionTypeConstants() {
        assertEquals("DEPOSIT", TransactionLog.TYPE_DEPOSIT);
        assertEquals("ROULETTE_WIN", TransactionLog.TYPE_ROULETTE_WIN);
        assertEquals("LETTER_BONUS", TransactionLog.TYPE_LETTER_BONUS);
        assertEquals("DAILY_LOGIN_SPIN", TransactionLog.TYPE_DAILY_LOGIN_SPIN);
        assertEquals("FIRST_DEPOSIT_SPIN", TransactionLog.TYPE_FIRST_DEPOSIT_SPIN);
        assertEquals("DEPOSIT_MISSION_SPIN", TransactionLog.TYPE_DEPOSIT_MISSION_SPIN);
        assertEquals("SPIN_CONSUMED", TransactionLog.TYPE_SPIN_CONSUMED);
        assertEquals("LETTER_COLLECTED", TransactionLog.TYPE_LETTER_COLLECTED);
    }
    
    @Test
    void testSettersAndGetters() {
        TransactionLog log = new TransactionLog();
        Long id = 1L;
        Long userId = 123L;
        String type = TransactionLog.TYPE_DEPOSIT;
        BigDecimal amount = new BigDecimal("100.00");
        String description = "Test transaction";
        
        log.setId(id);
        log.setUserId(userId);
        log.setTransactionType(type);
        log.setAmount(amount);
        log.setDescription(description);
        
        assertEquals(id, log.getId());
        assertEquals(userId, log.getUserId());
        assertEquals(type, log.getTransactionType());
        assertEquals(amount, log.getAmount());
        assertEquals(description, log.getDescription());
    }
    
    @Test
    void testEqualsAndHashCode() {
        TransactionLog log1 = new TransactionLog();
        log1.setId(1L);
        
        TransactionLog log2 = new TransactionLog();
        log2.setId(1L);
        
        TransactionLog log3 = new TransactionLog();
        log3.setId(2L);
        
        assertEquals(log1, log2);
        assertNotEquals(log1, log3);
        assertEquals(log1.hashCode(), log2.hashCode());
    }
    
    @Test
    void testToString() {
        TransactionLog log = new TransactionLog(123L, TransactionLog.TYPE_DEPOSIT, 
            new BigDecimal("50.00"), "Test deposit");
        
        String toString = log.toString();
        assertTrue(toString.contains("userId=123"));
        assertTrue(toString.contains("transactionType='DEPOSIT'"));
        assertTrue(toString.contains("amount=50.00"));
        assertTrue(toString.contains("description='Test deposit'"));
    }
}