package com.casino.roulette.controller;

import com.casino.roulette.dto.SpinResultDTO;
import com.casino.roulette.entity.RouletteSlot;
import com.casino.roulette.service.RouletteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouletteController.class)
class RouletteControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RouletteService rouletteService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private List<RouletteSlot> sampleSlots;
    private SpinResultDTO cashWinResult;
    private SpinResultDTO letterWinResult;
    
    @BeforeEach
    void setUp() {
        // Sample roulette slots
        RouletteSlot cashSlot = new RouletteSlot();
        cashSlot.setId(1L);
        cashSlot.setSlotType(RouletteSlot.SlotType.CASH);
        cashSlot.setSlotValue("10.00");
        cashSlot.setWeight(50);
        cashSlot.setActive(true);
        
        RouletteSlot letterSlot = new RouletteSlot();
        letterSlot.setId(2L);
        letterSlot.setSlotType(RouletteSlot.SlotType.LETTER);
        letterSlot.setSlotValue("A");
        letterSlot.setWeight(30);
        letterSlot.setActive(true);
        
        sampleSlots = Arrays.asList(cashSlot, letterSlot);
        
        // Sample spin results
        cashWinResult = new SpinResultDTO("CASH", "10.00", new BigDecimal("10.00"), 5);
        letterWinResult = new SpinResultDTO("LETTER", "A", "A", 4);
    }
    
    @Test
    void spinRoulette_WithValidUserId_ReturnsCashWin() throws Exception {
        // Given
        Long userId = 123L;
        when(rouletteService.spinRoulette(userId)).thenReturn(cashWinResult);
        
        // When & Then
        mockMvc.perform(post("/api/roulette/spin")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("CASH"))
                .andExpect(jsonPath("$.value").value("10.00"))
                .andExpect(jsonPath("$.cash").value(10.00))
                .andExpect(jsonPath("$.remainingSpins").value(5));
        
        verify(rouletteService).spinRoulette(userId);
    }
    
    @Test
    void spinRoulette_WithValidUserId_ReturnsLetterWin() throws Exception {
        // Given
        Long userId = 123L;
        when(rouletteService.spinRoulette(userId)).thenReturn(letterWinResult);
        
        // When & Then
        mockMvc.perform(post("/api/roulette/spin")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("LETTER"))
                .andExpect(jsonPath("$.value").value("A"))
                .andExpect(jsonPath("$.letter").value("A"))
                .andExpect(jsonPath("$.remainingSpins").value(4));
        
        verify(rouletteService).spinRoulette(userId);
    }
    
    @Test
    void spinRoulette_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/roulette/spin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("MISSING_HEADER"));
        
        verify(rouletteService, never()).spinRoulette(any());
    }
    
    @Test
    void spinRoulette_WithInvalidUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/roulette/spin")
                .header("X-User-Id", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(rouletteService, never()).spinRoulette(any());
    }
    
    @Test
    void spinRoulette_WithNoSpinsAvailable_ReturnsUnprocessableEntity() throws Exception {
        // Given
        Long userId = 123L;
        when(rouletteService.spinRoulette(userId))
                .thenThrow(new IllegalStateException("User has no available spins"));
        
        // When & Then
        mockMvc.perform(post("/api/roulette/spin")
                .header("X-User-Id", userId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("OPERATION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("User has no available spins"));
        
        verify(rouletteService).spinRoulette(userId);
    }
    
    @Test
    void getRouletteSlots_ReturnsSlotConfiguration() throws Exception {
        // Given
        when(rouletteService.getRouletteConfiguration()).thenReturn(sampleSlots);
        
        // When & Then
        mockMvc.perform(get("/api/roulette/slots"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slotType").value("CASH"))
                .andExpect(jsonPath("$[0].slotValue").value("10.00"))
                .andExpect(jsonPath("$[0].weight").value(50))
                .andExpect(jsonPath("$[1].slotType").value("LETTER"))
                .andExpect(jsonPath("$[1].slotValue").value("A"))
                .andExpect(jsonPath("$[1].weight").value(30));
        
        verify(rouletteService).getRouletteConfiguration();
    }
    
    @Test
    void updateRouletteSlots_WithValidSlots_ReturnsSuccess() throws Exception {
        // Given
        doNothing().when(rouletteService).updateRouletteSlots(any());
        String slotsJson = objectMapper.writeValueAsString(sampleSlots);
        
        // When & Then
        mockMvc.perform(put("/api/roulette/slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(slotsJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Roulette slots updated successfully"))
                .andExpect(jsonPath("$.slotsCount").value(2));
        
        verify(rouletteService).updateRouletteSlots(any());
    }
    
    @Test
    void updateRouletteSlots_WithInvalidSlots_ReturnsBadRequest() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Slot type cannot be null"))
                .when(rouletteService).updateRouletteSlots(any());
        String slotsJson = objectMapper.writeValueAsString(sampleSlots);
        
        // When & Then
        mockMvc.perform(put("/api/roulette/slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(slotsJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Slot type cannot be null"));
        
        verify(rouletteService).updateRouletteSlots(any());
    }
    
    @Test
    void getRouletteStatus_ReturnsStatusInformation() throws Exception {
        // Given
        when(rouletteService.isRouletteAvailable()).thenReturn(true);
        when(rouletteService.getTotalActiveWeight()).thenReturn(80L);
        when(rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.CASH)).thenReturn(3L);
        when(rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.LETTER)).thenReturn(2L);
        
        // When & Then
        mockMvc.perform(get("/api/roulette/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.totalWeight").value(80))
                .andExpect(jsonPath("$.cashSlotCount").value(3))
                .andExpect(jsonPath("$.letterSlotCount").value(2))
                .andExpect(jsonPath("$.totalSlotCount").value(5));
        
        verify(rouletteService).isRouletteAvailable();
        verify(rouletteService).getTotalActiveWeight();
        verify(rouletteService).getActiveSlotCountByType(RouletteSlot.SlotType.CASH);
        verify(rouletteService).getActiveSlotCountByType(RouletteSlot.SlotType.LETTER);
    }
    
    @Test
    void getSlotTypeStatistics_WithValidType_ReturnsStatistics() throws Exception {
        // Given
        when(rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.CASH)).thenReturn(3L);
        
        // When & Then
        mockMvc.perform(get("/api/roulette/stats/CASH"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slotType").value("CASH"))
                .andExpect(jsonPath("$.activeSlotCount").value(3));
        
        verify(rouletteService).getActiveSlotCountByType(RouletteSlot.SlotType.CASH);
    }
    
    @Test
    void getSlotTypeStatistics_WithInvalidType_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/roulette/stats/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_SLOT_TYPE"))
                .andExpect(jsonPath("$.message").value("Slot type must be either CASH or LETTER"))
                .andExpect(jsonPath("$.validTypes").isArray())
                .andExpect(jsonPath("$.validTypes[0]").value("CASH"))
                .andExpect(jsonPath("$.validTypes[1]").value("LETTER"));
        
        verify(rouletteService, never()).getActiveSlotCountByType(any());
    }
    
    @Test
    void getSlotTypeStatistics_WithLowercaseType_ReturnsStatistics() throws Exception {
        // Given
        when(rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.LETTER)).thenReturn(2L);
        
        // When & Then
        mockMvc.perform(get("/api/roulette/stats/letter"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slotType").value("LETTER"))
                .andExpect(jsonPath("$.activeSlotCount").value(2));
        
        verify(rouletteService).getActiveSlotCountByType(RouletteSlot.SlotType.LETTER);
    }
}