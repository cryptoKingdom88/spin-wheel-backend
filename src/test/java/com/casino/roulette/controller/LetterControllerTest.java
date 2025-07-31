package com.casino.roulette.controller;

import com.casino.roulette.dto.LetterCollectionDTO;
import com.casino.roulette.dto.LetterWordDTO;
import com.casino.roulette.service.LetterService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LetterController.class)
class LetterControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LetterService letterService;
    
    private List<LetterCollectionDTO> sampleCollection;
    private List<LetterWordDTO> sampleWords;
    
    @BeforeEach
    void setUp() {
        // Sample letter collection
        sampleCollection = Arrays.asList(
            new LetterCollectionDTO("A", 3),
            new LetterCollectionDTO("B", 1),
            new LetterCollectionDTO("C", 2)
        );
        
        // Sample words
        sampleWords = Arrays.asList(
            new LetterWordDTO(1L, "CAB", Map.of("C", 1, "A", 1, "B", 1), new BigDecimal("50.00"), true),
            new LetterWordDTO(2L, "ABBA", Map.of("A", 2, "B", 2), new BigDecimal("100.00"), false),
            new LetterWordDTO(3L, "ACE", Map.of("A", 1, "C", 1, "E", 1), new BigDecimal("75.00"), false)
        );
    }
    
    @Test
    void getUserLetterCollection_WithValidUserId_ReturnsCollection() throws Exception {
        // Given
        Long userId = 123L;
        when(letterService.getUserLetterCollection(userId)).thenReturn(sampleCollection);
        
        // When & Then
        mockMvc.perform(get("/api/letters/collection")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].letter").value("A"))
                .andExpect(jsonPath("$[0].count").value(3))
                .andExpect(jsonPath("$[1].letter").value("B"))
                .andExpect(jsonPath("$[1].count").value(1))
                .andExpect(jsonPath("$[2].letter").value("C"))
                .andExpect(jsonPath("$[2].count").value(2));
        
        verify(letterService).getUserLetterCollection(userId);
    }
    
    @Test
    void getUserLetterCollection_WithoutUserId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/letters/collection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("MISSING_HEADER"));
        
        verify(letterService, never()).getUserLetterCollection(any());
    }
    
    @Test
    void getAvailableWords_WithValidUserId_ReturnsWords() throws Exception {
        // Given
        Long userId = 123L;
        when(letterService.getAvailableWords(userId)).thenReturn(sampleWords);
        
        // When & Then
        mockMvc.perform(get("/api/letters/words")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].word").value("CAB"))
                .andExpect(jsonPath("$[0].canClaim").value(true))
                .andExpect(jsonPath("$[0].rewardAmount").value(50.00))
                .andExpect(jsonPath("$[1].canClaim").value(false));
        
        verify(letterService).getAvailableWords(userId);
    }
    
    @Test
    void getClaimableWords_WithValidUserId_ReturnsClaimableWords() throws Exception {
        // Given
        Long userId = 123L;
        List<LetterWordDTO> claimableWords = Arrays.asList(sampleWords.get(0)); // Only CAB is claimable
        when(letterService.getClaimableWords(userId)).thenReturn(claimableWords);
        
        // When & Then
        mockMvc.perform(get("/api/letters/words/claimable")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].word").value("CAB"))
                .andExpect(jsonPath("$[0].canClaim").value(true));
        
        verify(letterService).getClaimableWords(userId);
    }
    
    @Test
    void claimWordBonus_WithValidRequest_ReturnsSuccess() throws Exception {
        // Given
        Long userId = 123L;
        Long wordId = 1L;
        doNothing().when(letterService).claimWordBonus(userId, wordId);
        
        // When & Then
        mockMvc.perform(post("/api/letters/words/{wordId}/claim", wordId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Word bonus claimed successfully"))
                .andExpect(jsonPath("$.wordId").value(wordId));
        
        verify(letterService).claimWordBonus(userId, wordId);
    }
    
    @Test
    void claimWordBonus_WithInvalidWord_ReturnsBadRequest() throws Exception {
        // Given
        Long userId = 123L;
        Long wordId = 999L;
        doThrow(new IllegalArgumentException("Word not found: " + wordId))
                .when(letterService).claimWordBonus(userId, wordId);
        
        // When & Then
        mockMvc.perform(post("/api/letters/words/{wordId}/claim", wordId)
                .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Word not found: " + wordId));
        
        verify(letterService).claimWordBonus(userId, wordId);
    }
    
    @Test
    void claimWordBonus_WithInsufficientLetters_ReturnsUnprocessableEntity() throws Exception {
        // Given
        Long userId = 123L;
        Long wordId = 1L;
        doThrow(new IllegalStateException("User does not have sufficient letters to claim word: CAB"))
                .when(letterService).claimWordBonus(userId, wordId);
        
        // When & Then
        mockMvc.perform(post("/api/letters/words/{wordId}/claim", wordId)
                .header("X-User-Id", userId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("OPERATION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("User does not have sufficient letters to claim word: CAB"));
        
        verify(letterService).claimWordBonus(userId, wordId);
    }
    
    @Test
    void checkWordEligibility_WithValidRequest_ReturnsEligibility() throws Exception {
        // Given
        Long userId = 123L;
        Long wordId = 1L;
        when(letterService.canClaimWord(userId, wordId)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/letters/words/{wordId}/eligibility", wordId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.wordId").value(wordId))
                .andExpect(jsonPath("$.canClaim").value(true));
        
        verify(letterService).canClaimWord(userId, wordId);
    }
    
    @Test
    void getCollectionStatistics_WithValidUserId_ReturnsStatistics() throws Exception {
        // Given
        Long userId = 123L;
        when(letterService.getTotalLetterCount(userId)).thenReturn(6L);
        when(letterService.getDistinctLetterCount(userId)).thenReturn(3L);
        when(letterService.getDistinctLetters(userId)).thenReturn(Arrays.asList("A", "B", "C"));
        when(letterService.hasClaimableWords(userId)).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/letters/collection/stats")
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalLetterCount").value(6))
                .andExpect(jsonPath("$.distinctLetterCount").value(3))
                .andExpect(jsonPath("$.distinctLetters").isArray())
                .andExpect(jsonPath("$.distinctLetters.length()").value(3))
                .andExpect(jsonPath("$.hasClaimableWords").value(true));
        
        verify(letterService).getTotalLetterCount(userId);
        verify(letterService).getDistinctLetterCount(userId);
        verify(letterService).getDistinctLetters(userId);
        verify(letterService).hasClaimableWords(userId);
    }
    
    @Test
    void getLetterCount_WithValidLetter_ReturnsCount() throws Exception {
        // Given
        Long userId = 123L;
        String letter = "A";
        when(letterService.getLetterCount(userId, letter)).thenReturn(3);
        
        // When & Then
        mockMvc.perform(get("/api/letters/collection/{letter}", letter)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.letter").value("A"))
                .andExpect(jsonPath("$.count").value(3));
        
        verify(letterService).getLetterCount(userId, letter);
    }
    
    @Test
    void getLetterCount_WithInvalidLetter_ReturnsBadRequest() throws Exception {
        // Given
        Long userId = 123L;
        String invalidLetter = "AB"; // Too long
        
        // When & Then
        mockMvc.perform(get("/api/letters/collection/{letter}", invalidLetter)
                .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_LETTER"))
                .andExpect(jsonPath("$.message").value("Letter must be a single alphabetic character"));
        
        verify(letterService, never()).getLetterCount(any(), any());
    }
    
    @Test
    void checkLetterRequirement_WithValidRequest_ReturnsCheck() throws Exception {
        // Given
        Long userId = 123L;
        String letter = "A";
        Integer requiredCount = 2;
        when(letterService.hasAtLeastLetters(userId, letter, requiredCount)).thenReturn(true);
        when(letterService.getLetterCount(userId, letter)).thenReturn(3);
        
        // When & Then
        mockMvc.perform(get("/api/letters/collection/{letter}/check/{requiredCount}", letter, requiredCount)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.letter").value("A"))
                .andExpect(jsonPath("$.requiredCount").value(2))
                .andExpect(jsonPath("$.actualCount").value(3))
                .andExpect(jsonPath("$.hasSufficient").value(true));
        
        verify(letterService).hasAtLeastLetters(userId, letter, requiredCount);
        verify(letterService).getLetterCount(userId, letter);
    }
    
    @Test
    void getAllAvailableWords_ReturnsAllWords() throws Exception {
        // Given
        when(letterService.getAllAvailableWords()).thenReturn(sampleWords);
        
        // When & Then
        mockMvc.perform(get("/api/letters/words/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].word").value("CAB"))
                .andExpect(jsonPath("$[1].word").value("ABBA"))
                .andExpect(jsonPath("$[2].word").value("ACE"));
        
        verify(letterService).getAllAvailableWords();
    }
    
    @Test
    void claimWordBonus_WithInvalidWordId_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/letters/words/0/claim")
                .header("X-User-Id", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(letterService, never()).claimWordBonus(any(), any());
    }
    
    @Test
    void checkLetterRequirement_WithInvalidRequiredCount_ReturnsBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/letters/collection/A/check/0")
                .header("X-User-Id", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        
        verify(letterService, never()).hasAtLeastLetters(any(), any(), any());
    }
}