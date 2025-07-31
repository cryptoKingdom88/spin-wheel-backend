package com.casino.roulette.controller;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.service.MissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MissionController.class)
class MissionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MissionService missionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private List<MissionDTO> sampleMissions;
    
    @BeforeEach
    void setUp() {
        sampleMissions = Arrays.asList(
            new MissionDTO(1L, "Small Deposit", "Deposit $50-$99 for 1 spin", 1, true, 0, 50),
            new MissionDTO(2L, "Medium Deposit", "Deposit $100-$199 for 1 spin", 1, false, 100, 100),
            new MissionDTO(3L, "Large Deposit", "Deposit $500+ for 2 spins", 2, true, 5, 500)
        );
    }
    
    @Test
    void getAvailableMissions_WithValidUserId_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        when(missionService.getAvailableMissions(userId)).thenReturn(sampleMissions);
        
        // When & Then
        mockMvc.perform(get("/api/missions")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Small Deposit"))
                .andExpect(jsonPath("$[0].canClaim").value(true))
                .andExpect(jsonPath("$[1].canClaim").value(false))
                .andExpect(jsonPath("$[2].spinsAvailable").value(2));
        
        verify(missionService).getAvailableMissions(userId);
    }
    
    @Test
    void getAvailableMissions_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/missions"))
                .andExpect(status().isBadRequest());
        
        verify(missionService, never()).getAvailableMissions(any());
    }
    
    @Test
    void getAvailableMissions_WithInvalidUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/missions")
                .header("X-User-Id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(missionService, never()).getAvailableMissions(any());
    }
    
    @Test
    void claimMissionReward_WithValidRequest_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        Long missionId = 1L;
        doNothing().when(missionService).claimMissionReward(userId, missionId);
        
        // When & Then
        mockMvc.perform(post("/api/missions/{missionId}/claim", missionId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mission reward claimed successfully"))
                .andExpect(jsonPath("$.missionId").value(missionId));
        
        verify(missionService).claimMissionReward(userId, missionId);
    }
    
    @Test
    void claimMissionReward_WithInvalidMission_ReturnsBadRequest() throws Exception {
        // Given
        Long userId = 123L;
        Long missionId = 999L;
        doThrow(new IllegalArgumentException("Mission not found: " + missionId))
                .when(missionService).claimMissionReward(userId, missionId);
        
        // When & Then
        mockMvc.perform(post("/api/missions/{missionId}/claim", missionId)
                .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Mission not found: " + missionId));
        
        verify(missionService).claimMissionReward(userId, missionId);
    }
    
    @Test
    void claimMissionReward_WithMaxClaimsReached_ReturnsUnprocessableEntity() throws Exception {
        // Given
        Long userId = 123L;
        Long missionId = 1L;
        doThrow(new IllegalStateException("User has reached maximum claims for mission: " + missionId))
                .when(missionService).claimMissionReward(userId, missionId);
        
        // When & Then
        mockMvc.perform(post("/api/missions/{missionId}/claim", missionId)
                .header("X-User-Id", userId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("MISSION_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("User has reached maximum claims for mission: " + missionId));
        
        verify(missionService).claimMissionReward(userId, missionId);
    }
    
    @Test
    void claimMissionReward_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/missions/1/claim"))
                .andExpect(status().isBadRequest());
        
        verify(missionService, never()).claimMissionReward(any(), any());
    }
    
    @Test
    void claimMissionReward_WithInvalidMissionId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/missions/0/claim")
                .header("X-User-Id", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(missionService, never()).claimMissionReward(any(), any());
    }
    
    @Test
    void getMissionProgress_WithValidUserId_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        when(missionService.getAvailableMissions(userId)).thenReturn(sampleMissions);
        when(missionService.getTotalAvailableSpins(userId)).thenReturn(3);
        when(missionService.hasClaimableMissions(userId)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/missions/progress")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.missions").isArray())
                .andExpect(jsonPath("$.missions.length()").value(3))
                .andExpect(jsonPath("$.totalAvailableSpins").value(3))
                .andExpect(jsonPath("$.hasClaimableMissions").value(true));
        
        verify(missionService).getAvailableMissions(userId);
        verify(missionService).getTotalAvailableSpins(userId);
        verify(missionService).hasClaimableMissions(userId);
    }
    
    @Test
    void checkMissionEligibility_WithValidRequest_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        Long missionId = 1L;
        when(missionService.isUserEligibleForMission(userId, missionId)).thenReturn(true);
        when(missionService.getRemainingClaims(userId, missionId)).thenReturn(45);
        
        // When & Then
        mockMvc.perform(get("/api/missions/{missionId}/eligibility", missionId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.missionId").value(missionId))
                .andExpect(jsonPath("$.isEligible").value(true))
                .andExpect(jsonPath("$.remainingClaims").value(45));
        
        verify(missionService).isUserEligibleForMission(userId, missionId);
        verify(missionService).getRemainingClaims(userId, missionId);
    }
    
    @Test
    void checkMissionEligibility_WithInvalidMissionId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/missions/0/eligibility")
                .header("X-User-Id", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(missionService, never()).isUserEligibleForMission(any(), any());
        verify(missionService, never()).getRemainingClaims(any(), any());
    }
    
    @Test
    void checkMissionEligibility_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/missions/1/eligibility"))
                .andExpect(status().isBadRequest());
        
        verify(missionService, never()).isUserEligibleForMission(any(), any());
        verify(missionService, never()).getRemainingClaims(any(), any());
    }

    @Test
    void getAvailableMissions_WithNonExistentUser_ReturnsBasicMissionList() throws Exception {
        // Given
        Long nonExistentUserId = 999L;
        List<MissionDTO> basicMissions = Arrays.asList(
            new MissionDTO(1L, "Small Deposit", "Deposit $50-$99 for 1 spin", 1, false, 0, 50),
            new MissionDTO(2L, "Medium Deposit", "Deposit $100-$199 for 1 spin", 1, false, 0, 100)
        );
        
        when(missionService.getAvailableMissions(nonExistentUserId))
            .thenThrow(new com.casino.roulette.exception.UserNotFoundException(nonExistentUserId));
        when(missionService.getBasicMissionList()).thenReturn(basicMissions);
        
        // When & Then
        mockMvc.perform(get("/api/missions")
                .header("X-User-Id", nonExistentUserId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Small Deposit"))
                .andExpect(jsonPath("$[0].canClaim").value(false))
                .andExpect(jsonPath("$[1].name").value("Medium Deposit"))
                .andExpect(jsonPath("$[1].canClaim").value(false));
        
        verify(missionService).getAvailableMissions(nonExistentUserId);
        verify(missionService).getBasicMissionList();
    }

    @Test
    void getAvailableMissions_WithoutUserId_ReturnsBasicMissionList() throws Exception {
        // Given
        List<MissionDTO> basicMissions = Arrays.asList(
            new MissionDTO(1L, "Small Deposit", "Deposit $50-$99 for 1 spin", 1, false, 0, 50),
            new MissionDTO(-1L, "Daily Login", "Login daily for 1 spin", 1, false, 0, 1)
        );
        
        when(missionService.getBasicMissionList()).thenReturn(basicMissions);
        
        // When & Then
        mockMvc.perform(get("/api/missions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Small Deposit"))
                .andExpect(jsonPath("$[0].canClaim").value(false))
                .andExpect(jsonPath("$[1].name").value("Daily Login"))
                .andExpect(jsonPath("$[1].id").value(-1));
        
        verify(missionService).getBasicMissionList();
        verify(missionService, never()).getAvailableMissions(any());
    }
}