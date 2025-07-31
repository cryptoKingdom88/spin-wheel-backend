package com.casino.roulette.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMissionProgressTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Test
    void testValidUserMissionProgress() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        progress.setClaimsUsed(5);
        progress.setLastClaimDate(LocalDateTime.now());
        
        Set<ConstraintViolation<UserMissionProgress>> violations = validator.validate(progress);
        assertTrue(violations.isEmpty());
    }
    
    @Test
    void testUserMissionProgressWithNullUserId() {
        UserMissionProgress progress = new UserMissionProgress();
        progress.setUserId(null);
        progress.setMissionId(1L);
        progress.setClaimsUsed(5);
        
        Set<ConstraintViolation<UserMissionProgress>> violations = validator.validate(progress);
        assertEquals(1, violations.size());
        assertEquals("User ID cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testUserMissionProgressWithNullMissionId() {
        UserMissionProgress progress = new UserMissionProgress();
        progress.setUserId(1L);
        progress.setMissionId(null);
        progress.setClaimsUsed(5);
        
        Set<ConstraintViolation<UserMissionProgress>> violations = validator.validate(progress);
        assertEquals(1, violations.size());
        assertEquals("Mission ID cannot be null", violations.iterator().next().getMessage());
    }
    
    @Test
    void testUserMissionProgressWithNegativeClaimsUsed() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        progress.setClaimsUsed(-1);
        
        Set<ConstraintViolation<UserMissionProgress>> violations = validator.validate(progress);
        assertEquals(1, violations.size());
        assertEquals("Claims used cannot be negative", violations.iterator().next().getMessage());
    }
    
    @Test
    void testDefaultValues() {
        UserMissionProgress progress = new UserMissionProgress();
        assertEquals(0, progress.getClaimsUsed());
        assertNull(progress.getLastClaimDate());
    }
    
    @Test
    void testConstructorWithRequiredFields() {
        Long userId = 123L;
        Long missionId = 456L;
        
        UserMissionProgress progress = new UserMissionProgress(userId, missionId);
        
        assertEquals(userId, progress.getUserId());
        assertEquals(missionId, progress.getMissionId());
        assertEquals(0, progress.getClaimsUsed());
        assertNull(progress.getLastClaimDate());
    }
    
    @Test
    void testAddAvailableClaimsAndClaimAll() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        assertEquals(0, progress.getClaimsUsed());
        assertEquals(0, progress.getAvailableClaims());
        assertNull(progress.getLastClaimDate());
        
        // Add available claims
        progress.addAvailableClaims(2);
        assertEquals(0, progress.getClaimsUsed());
        assertEquals(2, progress.getAvailableClaims());
        
        // Claim all available
        LocalDateTime beforeClaim = LocalDateTime.now();
        Integer claimedCount = progress.claimAllAvailable();
        LocalDateTime afterClaim = LocalDateTime.now();
        
        assertEquals(2, claimedCount);
        assertEquals(2, progress.getClaimsUsed());
        assertEquals(0, progress.getAvailableClaims());
        assertNotNull(progress.getLastClaimDate());
        assertTrue(progress.getLastClaimDate().isAfter(beforeClaim) || 
                  progress.getLastClaimDate().isEqual(beforeClaim));
        assertTrue(progress.getLastClaimDate().isBefore(afterClaim) || 
                  progress.getLastClaimDate().isEqual(afterClaim));
    }
    
    @Test
    void testCanClaimWithMaxClaims() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        progress.setClaimsUsed(3);
        
        // Test with max claims higher than current claims
        assertTrue(progress.canClaim(5));
        
        // Test with max claims equal to current claims
        assertFalse(progress.canClaim(3));
        
        // Test with max claims lower than current claims
        assertFalse(progress.canClaim(2));
        
        // Test with null max claims (unlimited)
        assertTrue(progress.canClaim(null));
    }
    
    @Test
    void testCanClaimWithZeroClaims() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        progress.setClaimsUsed(0);
        
        assertTrue(progress.canClaim(1));
        assertTrue(progress.canClaim(5));
        assertTrue(progress.canClaim(null));
    }
    
    @Test
    void testGetRemainingClaims() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        progress.setClaimsUsed(3);
        
        // Test with max claims higher than current claims
        assertEquals(2, progress.getRemainingClaims(5));
        
        // Test with max claims equal to current claims
        assertEquals(0, progress.getRemainingClaims(3));
        
        // Test with max claims lower than current claims
        assertEquals(0, progress.getRemainingClaims(2));
        
        // Test with null max claims (unlimited)
        assertEquals(Integer.MAX_VALUE, progress.getRemainingClaims(null));
    }
    
    @Test
    void testGetRemainingClaimsWithZeroClaims() {
        UserMissionProgress progress = new UserMissionProgress(1L, 1L);
        progress.setClaimsUsed(0);
        
        assertEquals(5, progress.getRemainingClaims(5));
        assertEquals(1, progress.getRemainingClaims(1));
        assertEquals(Integer.MAX_VALUE, progress.getRemainingClaims(null));
    }
    
    @Test
    void testSettersAndGetters() {
        UserMissionProgress progress = new UserMissionProgress();
        Long id = 1L;
        Long userId = 123L;
        Long missionId = 456L;
        Integer claimsUsed = 10;
        LocalDateTime lastClaimDate = LocalDateTime.now();
        
        progress.setId(id);
        progress.setUserId(userId);
        progress.setMissionId(missionId);
        progress.setClaimsUsed(claimsUsed);
        progress.setLastClaimDate(lastClaimDate);
        
        assertEquals(id, progress.getId());
        assertEquals(userId, progress.getUserId());
        assertEquals(missionId, progress.getMissionId());
        assertEquals(claimsUsed, progress.getClaimsUsed());
        assertEquals(lastClaimDate, progress.getLastClaimDate());
    }
    
    @Test
    void testEqualsAndHashCode() {
        UserMissionProgress progress1 = new UserMissionProgress();
        progress1.setId(1L);
        
        UserMissionProgress progress2 = new UserMissionProgress();
        progress2.setId(1L);
        
        UserMissionProgress progress3 = new UserMissionProgress();
        progress3.setId(2L);
        
        assertEquals(progress1, progress2);
        assertNotEquals(progress1, progress3);
        assertEquals(progress1.hashCode(), progress2.hashCode());
    }
    
    @Test
    void testToString() {
        UserMissionProgress progress = new UserMissionProgress(123L, 456L);
        progress.setClaimsUsed(5);
        progress.setLastClaimDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        
        String toString = progress.toString();
        assertTrue(toString.contains("userId=123"));
        assertTrue(toString.contains("missionId=456"));
        assertTrue(toString.contains("claimsUsed=5"));
        assertTrue(toString.contains("lastClaimDate=2024-01-15T10:30"));
    }
}