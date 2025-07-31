package com.casino.roulette.controller;

import com.casino.roulette.entity.DepositMission;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.service.MissionService;
import com.casino.roulette.service.TransactionService;
import com.casino.roulette.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepositController.class)
class DepositControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MissionService missionService;
    
    @MockBean
    private TransactionService transactionService;
    
    @MockBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User sampleUser;
    private List<DepositMission> sampleMissions;
    private List<TransactionLog> sampleTransactions;
    
    @BeforeEach
    void setUp() {
        // Sample user
        sampleUser = new User();
        sampleUser.setId(123L);
        sampleUser.setCashBalance(new BigDecimal("150.00"));
        sampleUser.setFirstDepositBonusUsed(false);
        
        // Sample missions
        DepositMission mission1 = new DepositMission();
        mission1.setId(1L);
        mission1.setName("Small Deposit");
        mission1.setMinAmount(new BigDecimal("50.00"));
        mission1.setMaxAmount(new BigDecimal("99.99"));
        mission1.setSpinsGranted(1);
        mission1.setMaxClaims(50);
        mission1.setActive(true);
        
        DepositMission mission2 = new DepositMission();
        mission2.setId(2L);
        mission2.setName("Medium Deposit");
        mission2.setMinAmount(new BigDecimal("100.00"));
        mission2.setMaxAmount(new BigDecimal("199.99"));
        mission2.setSpinsGranted(1);
        mission2.setMaxClaims(100);
        mission2.setActive(true);
        
        sampleMissions = Arrays.asList(mission1, mission2);
        
        // Sample transactions
        TransactionLog deposit1 = new TransactionLog();
        deposit1.setId(1L);
        deposit1.setUserId(123L);
        deposit1.setTransactionType("DEPOSIT");
        deposit1.setAmount(new BigDecimal("100.00"));
        deposit1.setDescription("Deposit");
        
        TransactionLog deposit2 = new TransactionLog();
        deposit2.setId(2L);
        deposit2.setUserId(123L);
        deposit2.setTransactionType("DEPOSIT");
        deposit2.setAmount(new BigDecimal("50.00"));
        deposit2.setDescription("Deposit");
        
        sampleTransactions = Arrays.asList(deposit1, deposit2);
    }
    
    @Test
    void processDeposit_WithValidRequest_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        BigDecimal depositAmount = new BigDecimal("100.00");
        DepositController.DepositRequest request = new DepositController.DepositRequest(depositAmount);
        
        when(missionService.getMissionsForAmount(depositAmount)).thenReturn(sampleMissions);
        doNothing().when(missionService).processDepositMissions(userId, depositAmount);
        when(userService.getUser(userId)).thenReturn(sampleUser);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When & Then
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deposit processed successfully"))
                .andExpect(jsonPath("$.depositAmount").value(100.00))
                .andExpect(jsonPath("$.newBalance").value(150.00))
                .andExpect(jsonPath("$.eligibleMissions").value(2))
                .andExpect(jsonPath("$.missionIds").isArray())
                .andExpect(jsonPath("$.missionIds.length()").value(2));
        
        verify(missionService).getMissionsForAmount(depositAmount);
        verify(missionService).processDepositMissions(userId, depositAmount);
        verify(userService).getUser(userId);
    }
    
    @Test
    void processDeposit_WithInvalidAmount_ReturnsBadRequest() throws Exception {
        // Given
        Long userId = 123L;
        DepositController.DepositRequest request = new DepositController.DepositRequest(new BigDecimal("0"));
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When & Then
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
        
        verify(missionService, never()).processDepositMissions(any(), any());
    }
    
    @Test
    void processDeposit_WithoutUserId_ReturnsBadRequest() throws Exception {
        // Given
        DepositController.DepositRequest request = new DepositController.DepositRequest(new BigDecimal("100.00"));
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When & Then
        mockMvc.perform(post("/api/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("MISSING_HEADER"));
        
        verify(missionService, never()).processDepositMissions(any(), any());
    }
    
    @Test
    void processDeposit_WithServiceException_ReturnsBadRequest() throws Exception {
        // Given
        Long userId = 123L;
        BigDecimal depositAmount = new BigDecimal("100.00");
        DepositController.DepositRequest request = new DepositController.DepositRequest(depositAmount);
        
        when(missionService.getMissionsForAmount(depositAmount)).thenReturn(sampleMissions);
        doThrow(new IllegalArgumentException("Deposit amount must be positive"))
                .when(missionService).processDepositMissions(userId, depositAmount);
        
        String requestJson = objectMapper.writeValueAsString(request);
        
        // When & Then
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Deposit amount must be positive"));
        
        verify(missionService).getMissionsForAmount(depositAmount);
        verify(missionService).processDepositMissions(userId, depositAmount);
    }
    
    @Test
    void getDepositHistory_WithValidUserId_ReturnsHistory() throws Exception {
        // Given
        Long userId = 123L;
        when(transactionService.getTransactionHistory(userId)).thenReturn(sampleTransactions);
        
        // When & Then
        mockMvc.perform(get("/api/deposits/history")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.deposits").isArray())
                .andExpect(jsonPath("$.deposits.length()").value(2))
                .andExpect(jsonPath("$.totalDeposited").value(150.00))
                .andExpect(jsonPath("$.depositCount").value(2));
        
        verify(transactionService).getTransactionHistory(userId);
    }
    
    @Test
    void previewEligibleMissions_WithValidAmount_ReturnsMissions() throws Exception {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        when(missionService.getMissionsForAmount(amount)).thenReturn(sampleMissions);
        
        // When & Then
        mockMvc.perform(get("/api/deposits/missions/preview")
                .param("amount", amount.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.depositAmount").value(100.00))
                .andExpect(jsonPath("$.eligibleMissions").isArray())
                .andExpect(jsonPath("$.eligibleMissions.length()").value(2))
                .andExpect(jsonPath("$.missionCount").value(2))
                .andExpect(jsonPath("$.totalSpinsAvailable").value(2));
        
        verify(missionService).getMissionsForAmount(amount);
    }
    
    @Test
    void previewEligibleMissions_WithInvalidAmount_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/deposits/missions/preview")
                .param("amount", "0"))
                .andExpect(status().isBadRequest());
        
        verify(missionService, never()).getMissionsForAmount(any());
    }
    
    @Test
    void getFirstDepositStatus_WithNewUser_ReturnsEligible() throws Exception {
        // Given
        Long userId = 123L;
        when(userService.getUser(userId)).thenReturn(sampleUser);
        
        // When & Then
        mockMvc.perform(get("/api/deposits/first-deposit/status")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasUsedFirstDepositBonus").value(false))
                .andExpect(jsonPath("$.isEligibleForFirstDepositBonus").value(true));
        
        verify(userService).getUser(userId);
    }
    
    @Test
    void getFirstDepositStatus_WithExistingUser_ReturnsNotEligible() throws Exception {
        // Given
        Long userId = 123L;
        sampleUser.setFirstDepositBonusUsed(true);
        when(userService.getUser(userId)).thenReturn(sampleUser);
        
        // When & Then
        mockMvc.perform(get("/api/deposits/first-deposit/status")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasUsedFirstDepositBonus").value(true))
                .andExpect(jsonPath("$.isEligibleForFirstDepositBonus").value(false));
        
        verify(userService).getUser(userId);
    }
    
    @Test
    void getDepositStatistics_WithValidUserId_ReturnsStatistics() throws Exception {
        // Given
        Long userId = 123L;
        when(transactionService.getTransactionHistory(userId)).thenReturn(sampleTransactions);
        when(userService.getUser(userId)).thenReturn(sampleUser);
        
        // When & Then
        mockMvc.perform(get("/api/deposits/stats")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalDeposited").value(150.00))
                .andExpect(jsonPath("$.depositCount").value(2))
                .andExpect(jsonPath("$.averageDeposit").value(75.00))
                .andExpect(jsonPath("$.currentBalance").value(150.00))
                .andExpect(jsonPath("$.hasUsedFirstDepositBonus").value(false));
        
        verify(transactionService).getTransactionHistory(userId);
        verify(userService).getUser(userId);
    }
    
    @Test
    void getDepositHistory_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/deposits/history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("MISSING_HEADER"));
        
        verify(transactionService, never()).getTransactionHistory(any());
    }
    
    @Test
    void getDepositStatistics_WithInvalidUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/deposits/stats")
                .header("X-User-Id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(transactionService, never()).getTransactionHistory(any());
        verify(userService, never()).getUser(any());
    }
}