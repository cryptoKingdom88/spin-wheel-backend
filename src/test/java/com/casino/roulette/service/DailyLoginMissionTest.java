package com.casino.roulette.service;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.entity.DailyLoginMission;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.DailyLoginMissionRepository;
import com.casino.roulette.repository.DepositMissionRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserMissionProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyLoginMissionTest {

    @Mock
    private DailyLoginMissionRepository dailyLoginMissionRepository;

    @Mock
    private DepositMissionRepository depositMissionRepository;

    @Mock
    private UserMissionProgressRepository userMissionProgressRepository;

    @Mock
    private TransactionLogRepository transactionLogRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MissionService missionService;

    @Test
    void getAvailableMissions_NewUser_CanClaimDailyLogin() {
        // Given
        Long newUserId = 123L;
        User newUser = new User(newUserId);
        // lastDailyMissionClaim is null for new user
        
        DailyLoginMission dailyMission = new DailyLoginMission();
        dailyMission.setId(1L);
        dailyMission.setName("Daily Login Bonus");
        dailyMission.setDescription("Login daily to get 1 free spin");
        dailyMission.setSpinsGranted(1);
        dailyMission.setActive(true);

        when(userService.getOrCreateUser(newUserId)).thenReturn(newUser);
        when(userService.getUser(newUserId)).thenReturn(newUser);
        when(dailyLoginMissionRepository.findFirstByActiveTrue()).thenReturn(Optional.of(dailyMission));
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(List.of());

        // When
        List<MissionDTO> missions = missionService.getAvailableMissions(newUserId);

        // Then
        assertNotNull(missions);
        assertFalse(missions.isEmpty());
        
        // Find daily login mission
        Optional<MissionDTO> dailyMissionOpt = missions.stream()
            .filter(m -> m.getId().equals(-1L))
            .findFirst();
        
        assertTrue(dailyMissionOpt.isPresent());
        MissionDTO dailyMissionDTO = dailyMissionOpt.get();
        
        assertEquals("Daily Login Bonus", dailyMissionDTO.getName());
        assertTrue(dailyMissionDTO.getCanClaim());
        assertEquals(1, dailyMissionDTO.getSpinsAvailable());
        assertTrue(dailyMissionDTO.getDescription().contains("Available now!"));
        
        verify(userService).getOrCreateUser(newUserId);
        verify(userService).getUser(newUserId);
    }

    @Test
    void getAvailableMissions_UserAlreadyClaimedToday_CannotClaimDailyLogin() {
        // Given
        Long userId = 123L;
        User user = new User(userId);
        user.setLastDailyMissionClaim(LocalDateTime.now()); // Already claimed today
        
        DailyLoginMission dailyMission = new DailyLoginMission();
        dailyMission.setId(1L);
        dailyMission.setName("Daily Login Bonus");
        dailyMission.setDescription("Login daily to get 1 free spin");
        dailyMission.setSpinsGranted(1);
        dailyMission.setActive(true);

        when(userService.getOrCreateUser(userId)).thenReturn(user);
        when(userService.getUser(userId)).thenReturn(user);
        when(dailyLoginMissionRepository.findFirstByActiveTrue()).thenReturn(Optional.of(dailyMission));
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(List.of());

        // When
        List<MissionDTO> missions = missionService.getAvailableMissions(userId);

        // Then
        assertNotNull(missions);
        assertFalse(missions.isEmpty());
        
        // Find daily login mission
        Optional<MissionDTO> dailyMissionOpt = missions.stream()
            .filter(m -> m.getId().equals(-1L))
            .findFirst();
        
        assertTrue(dailyMissionOpt.isPresent());
        MissionDTO dailyMissionDTO = dailyMissionOpt.get();
        
        assertEquals("Daily Login Bonus", dailyMissionDTO.getName());
        assertFalse(dailyMissionDTO.getCanClaim());
        assertTrue(dailyMissionDTO.getDescription().contains("Already claimed today"));
    }

    @Test
    void getAvailableMissions_UserClaimedYesterday_CanClaimDailyLogin() {
        // Given
        Long userId = 123L;
        User user = new User(userId);
        user.setLastDailyMissionClaim(LocalDateTime.now().minusDays(1)); // Claimed yesterday
        
        DailyLoginMission dailyMission = new DailyLoginMission();
        dailyMission.setId(1L);
        dailyMission.setName("Daily Login Bonus");
        dailyMission.setDescription("Login daily to get 1 free spin");
        dailyMission.setSpinsGranted(1);
        dailyMission.setActive(true);

        when(userService.getOrCreateUser(userId)).thenReturn(user);
        when(userService.getUser(userId)).thenReturn(user);
        when(dailyLoginMissionRepository.findFirstByActiveTrue()).thenReturn(Optional.of(dailyMission));
        when(depositMissionRepository.findActiveOrderedByMinAmount()).thenReturn(List.of());

        // When
        List<MissionDTO> missions = missionService.getAvailableMissions(userId);

        // Then
        assertNotNull(missions);
        assertFalse(missions.isEmpty());
        
        // Find daily login mission
        Optional<MissionDTO> dailyMissionOpt = missions.stream()
            .filter(m -> m.getId().equals(-1L))
            .findFirst();
        
        assertTrue(dailyMissionOpt.isPresent());
        MissionDTO dailyMissionDTO = dailyMissionOpt.get();
        
        assertEquals("Daily Login Bonus", dailyMissionDTO.getName());
        assertTrue(dailyMissionDTO.getCanClaim());
        assertTrue(dailyMissionDTO.getDescription().contains("Available now!"));
    }
}