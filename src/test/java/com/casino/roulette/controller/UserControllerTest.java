package com.casino.roulette.controller;

import com.casino.roulette.entity.User;
import com.casino.roulette.exception.UserNotFoundException;
import com.casino.roulette.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    private User sampleUser;
    
    @BeforeEach
    void setUp() {
        sampleUser = new User(123L);
        sampleUser.setCashBalance(new BigDecimal("150.50"));
        sampleUser.setAvailableSpins(5);
        sampleUser.setFirstDepositBonusUsed(true);
        sampleUser.setLastDailyLogin(LocalDateTime.of(2025, 7, 31, 10, 30));
    }
    
    @Test
    void getAvailableSpins_WithValidUserId_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        when(userService.validateUserExists(userId)).thenReturn(sampleUser);
        
        // When & Then
        mockMvc.perform(get("/api/users/spins")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.availableSpins").value(5))
                .andExpect(jsonPath("$.canSpin").value(true));
        
        verify(userService).validateUserExists(userId);
    }
    
    @Test
    void getAvailableSpins_WithZeroSpins_ReturnsCanSpinFalse() throws Exception {
        // Given
        Long userId = 123L;
        sampleUser.setAvailableSpins(0);
        when(userService.validateUserExists(userId)).thenReturn(sampleUser);
        
        // When & Then
        mockMvc.perform(get("/api/users/spins")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.availableSpins").value(0))
                .andExpect(jsonPath("$.canSpin").value(false));
        
        verify(userService).validateUserExists(userId);
    }
    
    @Test
    void getAvailableSpins_WithNonExistentUser_ReturnsNotFound() throws Exception {
        // Given
        Long userId = 999L;
        when(userService.validateUserExists(userId))
            .thenThrow(new UserNotFoundException(userId));
        
        // When & Then
        mockMvc.perform(get("/api/users/spins")
                .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.userId").value(userId));
        
        verify(userService).validateUserExists(userId);
    }
    
    @Test
    void getAvailableSpins_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/spins"))
                .andExpect(status().isBadRequest());
        
        verifyNoInteractions(userService);
    }
    
    @Test
    void getUserInfo_WithValidUserId_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        when(userService.validateUserExists(userId)).thenReturn(sampleUser);
        
        // When & Then
        mockMvc.perform(get("/api/users/info")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.cashBalance").value(150.50))
                .andExpect(jsonPath("$.availableSpins").value(5))
                .andExpect(jsonPath("$.firstDepositBonusUsed").value(true))
                .andExpect(jsonPath("$.canSpin").value(true))
                .andExpect(jsonPath("$.lastDailyLogin").exists());
        
        verify(userService).validateUserExists(userId);
    }
    
    @Test
    void getUserInfo_WithNonExistentUser_ReturnsNotFound() throws Exception {
        // Given
        Long userId = 999L;
        when(userService.validateUserExists(userId))
            .thenThrow(new UserNotFoundException(userId));
        
        // When & Then
        mockMvc.perform(get("/api/users/info")
                .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
        
        verify(userService).validateUserExists(userId);
    }
}