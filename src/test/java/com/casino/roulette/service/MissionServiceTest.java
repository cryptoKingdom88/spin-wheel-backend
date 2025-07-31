package com.casino.roulette.service;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.entity.DepositMission;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.entity.UserMissionProgress;
import com.casino.roulette.repository.DepositMissionRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserMissionProgressRepository;
import com.casino.roulette.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {
    
    @Mock
    private DepositMissionRepository depositMissionRepository;
    
    @Mock
    private UserMissionProgressRepository userMissionProgressRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TransactionLogRepository transactionLogRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private MissionService missionService;
    
    private User testUser;
    private DepositMission testMission;
    private UserMissionProgress testProgress;
    private final Long TEST_USER_ID = 1L;
    private final Long TEST_MISSION_ID = 1L;
    
    @BeforeEach
    void setUp() {
        testUser = new User(TEST_USER_ID);
        testUser.setCashBalance(new BigDecimal("100.00"));
        
        testMission = new DepositMission("Test Mission", new BigDecimal("50.00"), 2, 5);
        testMission.setId(TEST_MISSION_ID);
        testMission.setMaxAmount(new BigDecimal("99.99"));
        testMission.setActive(true);
        
        testProgress = new UserMissionProgress(TEST_USER_ID, TEST_MISSION_ID);
        testProgress.setClaimsUsed(2);
    }
    
    @Test
    void getAvailableMissions_WithProgress_ReturnsCorrectMissions() {
        // Given
        List<DepositMission> missions = Arrays.asList(testMission);
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(missions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress));
        
        // When
        List<MissionDTO> result = missionService.getAvailableMissions(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        MissionDTO dto = result.get(0);
        assertEquals(TEST_MISSION_ID, dto.getId());
        assertEquals("Test Mission", dto.getName());
        assertEquals(Integer.valueOf(2), dto.getSpinsAvailable());
        assertTrue(dto.getCanClaim()); // 2 < 5 max claims
        assertEquals(Integer.valueOf(2), dto.getClaimsUsed());
        assertEquals(Integer.valueOf(5), dto.getMaxClaims());
        assertNotNull(dto.getDescription());
    }
    
    @Test
    void getAvailableMissions_WithoutProgress_ReturnsNewMission() {
        // Given
        List<DepositMission> missions = Arrays.asList(testMission);
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(missions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.empty());
        
        // When
        List<MissionDTO> result = missionService.getAvailableMissions(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        MissionDTO dto = result.get(0);
        assertEquals(TEST_MISSION_ID, dto.getId());
        assertTrue(dto.getCanClaim());
        assertEquals(Integer.valueOf(0), dto.getClaimsUsed());
    }
    
    @Test
    void getAvailableMissions_MaxClaimsReached_CannotClaim() {
        // Given
        testProgress.setClaimsUsed(5); // Reached max claims
        List<DepositMission> missions = Arrays.asList(testMission);
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(missions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress));
        
        // When
        List<MissionDTO> result = missionService.getAvailableMissions(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        MissionDTO dto = result.get(0);
        assertFalse(dto.getCanClaim());
        assertEquals(Integer.valueOf(5), dto.getClaimsUsed());
    }
    
    @Test
    void getAvailableMissions_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> missionService.getAvailableMissions(null)
        );
        assertEquals("User ID cannot be null", exception.getMessage());
    }
    
    @Test
    void claimMissionReward_ValidClaim_GrantsSpinsAndUpdatesProgress() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findById(TEST_MISSION_ID)).thenReturn(Optional.of(testMission));
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress));
        
        // When
        missionService.claimMissionReward(TEST_USER_ID, TEST_MISSION_ID);
        
        // Then
        verify(userService).grantSpins(TEST_USER_ID, 2, "Mission reward: Test Mission");
        
        ArgumentCaptor<UserMissionProgress> progressCaptor = ArgumentCaptor.forClass(UserMissionProgress.class);
        verify(userMissionProgressRepository).save(progressCaptor.capture());
        
        UserMissionProgress savedProgress = progressCaptor.getValue();
        assertEquals(Integer.valueOf(3), savedProgress.getClaimsUsed()); // Incremented from 2 to 3
        assertNotNull(savedProgress.getLastClaimDate());
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals(TransactionLog.TYPE_DEPOSIT_MISSION_SPIN, savedLog.getTransactionType());
    }
    
    @Test
    void claimMissionReward_NewProgress_CreatesProgressAndGrantsSpins() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findById(TEST_MISSION_ID)).thenReturn(Optional.of(testMission));
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.empty());
        
        // When
        missionService.claimMissionReward(TEST_USER_ID, TEST_MISSION_ID);
        
        // Then
        verify(userService).grantSpins(TEST_USER_ID, 2, "Mission reward: Test Mission");
        
        ArgumentCaptor<UserMissionProgress> progressCaptor = ArgumentCaptor.forClass(UserMissionProgress.class);
        verify(userMissionProgressRepository).save(progressCaptor.capture());
        
        UserMissionProgress savedProgress = progressCaptor.getValue();
        assertEquals(TEST_USER_ID, savedProgress.getUserId());
        assertEquals(TEST_MISSION_ID, savedProgress.getMissionId());
        assertEquals(Integer.valueOf(1), savedProgress.getClaimsUsed());
    }
    
    @Test
    void claimMissionReward_MissionNotFound_ThrowsException() {
        // Given
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findById(TEST_MISSION_ID)).thenReturn(Optional.empty());
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> missionService.claimMissionReward(TEST_USER_ID, TEST_MISSION_ID)
        );
        assertTrue(exception.getMessage().contains("Mission not found"));
    }
    
    @Test
    void claimMissionReward_InactiveMission_ThrowsException() {
        // Given
        testMission.setActive(false);
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findById(TEST_MISSION_ID)).thenReturn(Optional.of(testMission));
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> missionService.claimMissionReward(TEST_USER_ID, TEST_MISSION_ID)
        );
        assertTrue(exception.getMessage().contains("Mission is not active"));
    }
    
    @Test
    void claimMissionReward_MaxClaimsReached_ThrowsException() {
        // Given
        testProgress.setClaimsUsed(5); // Max claims reached
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findById(TEST_MISSION_ID)).thenReturn(Optional.of(testMission));
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress));
        
        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> missionService.claimMissionReward(TEST_USER_ID, TEST_MISSION_ID)
        );
        assertTrue(exception.getMessage().contains("reached maximum claims"));
    }
    
    @Test
    void processDepositMissions_EligibleDeposit_CreatesProgressAndLogsTransaction() {
        // Given
        BigDecimal depositAmount = new BigDecimal("75.00");
        List<DepositMission> eligibleMissions = Arrays.asList(testMission);
        
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findActiveByAmountRange(depositAmount)).thenReturn(eligibleMissions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.empty());
        
        // When
        missionService.processDepositMissions(TEST_USER_ID, depositAmount);
        
        // Then
        ArgumentCaptor<UserMissionProgress> progressCaptor = ArgumentCaptor.forClass(UserMissionProgress.class);
        verify(userMissionProgressRepository).save(progressCaptor.capture());
        
        UserMissionProgress savedProgress = progressCaptor.getValue();
        assertEquals(TEST_USER_ID, savedProgress.getUserId());
        assertEquals(TEST_MISSION_ID, savedProgress.getMissionId());
        assertEquals(Integer.valueOf(0), savedProgress.getClaimsUsed());
        
        verify(userService).updateCashBalance(TEST_USER_ID, depositAmount);
        
        ArgumentCaptor<TransactionLog> logCaptor = ArgumentCaptor.forClass(TransactionLog.class);
        verify(transactionLogRepository).save(logCaptor.capture());
        
        TransactionLog savedLog = logCaptor.getValue();
        assertEquals(TEST_USER_ID, savedLog.getUserId());
        assertEquals(TransactionLog.TYPE_DEPOSIT, savedLog.getTransactionType());
        assertEquals(depositAmount, savedLog.getAmount());
    }
    
    @Test
    void processDepositMissions_ExistingProgressNotMaxed_DoesNotCreateNewProgress() {
        // Given
        BigDecimal depositAmount = new BigDecimal("75.00");
        List<DepositMission> eligibleMissions = Arrays.asList(testMission);
        
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findActiveByAmountRange(depositAmount)).thenReturn(eligibleMissions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress)); // Existing progress with 2 claims
        
        // When
        missionService.processDepositMissions(TEST_USER_ID, depositAmount);
        
        // Then
        verify(userMissionProgressRepository, never()).save(any(UserMissionProgress.class));
        verify(userService).updateCashBalance(TEST_USER_ID, depositAmount);
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void processDepositMissions_MaxClaimsReached_DoesNotCreateProgress() {
        // Given
        BigDecimal depositAmount = new BigDecimal("75.00");
        testProgress.setClaimsUsed(5); // Max claims reached
        List<DepositMission> eligibleMissions = Arrays.asList(testMission);
        
        when(userService.getOrCreateUser(TEST_USER_ID)).thenReturn(testUser);
        when(depositMissionRepository.findActiveByAmountRange(depositAmount)).thenReturn(eligibleMissions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress));
        
        // When
        missionService.processDepositMissions(TEST_USER_ID, depositAmount);
        
        // Then
        verify(userMissionProgressRepository, never()).save(any(UserMissionProgress.class));
        verify(userService).updateCashBalance(TEST_USER_ID, depositAmount);
        verify(transactionLogRepository).save(any(TransactionLog.class));
    }
    
    @Test
    void processDepositMissions_InvalidAmount_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> missionService.processDepositMissions(TEST_USER_ID, BigDecimal.ZERO)
        );
        assertTrue(exception.getMessage().contains("Deposit amount must be positive"));
    }
    
    @Test
    void isUserEligibleForMission_ValidInput_ReturnsRepositoryResult() {
        // Given
        when(userMissionProgressRepository.isUserEligibleForMission(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(true);
        
        // When
        boolean result = missionService.isUserEligibleForMission(TEST_USER_ID, TEST_MISSION_ID);
        
        // Then
        assertTrue(result);
        verify(userMissionProgressRepository).isUserEligibleForMission(TEST_USER_ID, TEST_MISSION_ID);
    }
    
    @Test
    void isUserEligibleForMission_NullInput_ReturnsFalse() {
        // When
        boolean result1 = missionService.isUserEligibleForMission(null, TEST_MISSION_ID);
        boolean result2 = missionService.isUserEligibleForMission(TEST_USER_ID, null);
        
        // Then
        assertFalse(result1);
        assertFalse(result2);
        verify(userMissionProgressRepository, never()).isUserEligibleForMission(any(), any());
    }
    
    @Test
    void getRemainingClaims_ValidInput_ReturnsRepositoryResult() {
        // Given
        when(userMissionProgressRepository.getRemainingClaimsForMission(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(3);
        
        // When
        Integer result = missionService.getRemainingClaims(TEST_USER_ID, TEST_MISSION_ID);
        
        // Then
        assertEquals(Integer.valueOf(3), result);
        verify(userMissionProgressRepository).getRemainingClaimsForMission(TEST_USER_ID, TEST_MISSION_ID);
    }
    
    @Test
    void getRemainingClaims_NullInput_ReturnsZero() {
        // When
        Integer result1 = missionService.getRemainingClaims(null, TEST_MISSION_ID);
        Integer result2 = missionService.getRemainingClaims(TEST_USER_ID, null);
        
        // Then
        assertEquals(Integer.valueOf(0), result1);
        assertEquals(Integer.valueOf(0), result2);
        verify(userMissionProgressRepository, never()).getRemainingClaimsForMission(any(), any());
    }
    
    @Test
    void getMissionsForAmount_ValidAmount_ReturnsMatchingMissions() {
        // Given
        BigDecimal amount = new BigDecimal("75.00");
        List<DepositMission> missions = Arrays.asList(testMission);
        when(depositMissionRepository.findActiveByAmountRange(amount)).thenReturn(missions);
        
        // When
        List<DepositMission> result = missionService.getMissionsForAmount(amount);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMission, result.get(0));
        verify(depositMissionRepository).findActiveByAmountRange(amount);
    }
    
    @Test
    void getMissionsForAmount_InvalidAmount_ReturnsEmptyList() {
        // When
        List<DepositMission> result1 = missionService.getMissionsForAmount(null);
        List<DepositMission> result2 = missionService.getMissionsForAmount(BigDecimal.ZERO);
        List<DepositMission> result3 = missionService.getMissionsForAmount(new BigDecimal("-10"));
        
        // Then
        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
        assertTrue(result3.isEmpty());
        verify(depositMissionRepository, never()).findActiveByAmountRange(any());
    }
    
    @Test
    void getTotalAvailableSpins_WithClaimableMissions_ReturnsCorrectTotal() {
        // Given
        DepositMission mission2 = new DepositMission("Mission 2", new BigDecimal("100.00"), 3, 2);
        mission2.setId(2L);
        
        List<DepositMission> missions = Arrays.asList(testMission, mission2);
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(missions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress)); // 2 claims used, 3 remaining
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, 2L))
            .thenReturn(Optional.empty()); // No claims used, 2 remaining
        
        // When
        Integer result = missionService.getTotalAvailableSpins(TEST_USER_ID);
        
        // Then
        // Mission 1: 2 spins * 3 remaining claims = 6 spins
        // Mission 2: 3 spins * 2 remaining claims = 6 spins
        // Total: 12 spins
        assertEquals(Integer.valueOf(12), result);
    }
    
    @Test
    void hasClaimableMissions_WithClaimableMissions_ReturnsTrue() {
        // Given
        List<DepositMission> missions = Arrays.asList(testMission);
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(missions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress)); // 2 < 5, so can claim
        
        // When
        boolean result = missionService.hasClaimableMissions(TEST_USER_ID);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void hasClaimableMissions_NoClaimableMissions_ReturnsFalse() {
        // Given
        testProgress.setClaimsUsed(5); // Max claims reached
        List<DepositMission> missions = Arrays.asList(testMission);
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(missions);
        when(userMissionProgressRepository.findByUserIdAndMissionId(TEST_USER_ID, TEST_MISSION_ID))
            .thenReturn(Optional.of(testProgress));
        
        // When
        boolean result = missionService.hasClaimableMissions(TEST_USER_ID);
        
        // Then
        assertFalse(result);
    }
}