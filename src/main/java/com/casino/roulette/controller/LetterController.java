package com.casino.roulette.controller;

import com.casino.roulette.dto.LetterCollectionDTO;
import com.casino.roulette.dto.LetterWordDTO;
import com.casino.roulette.service.LetterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for letter collection and word bonus operations
 */
@RestController
@RequestMapping("/letters")
@CrossOrigin(originPatterns = "*", maxAge = 3600, allowCredentials = "false")
@Validated
@Tag(name = "Letters", description = "Letter collection and word bonus operations")
public class LetterController {
    
    private final LetterService letterService;
    
    @Autowired
    public LetterController(LetterService letterService) {
        this.letterService = letterService;
    }
    
    /**
     * Get user's letter collection
     * 
     * @param userId The user ID from request header
     * @return List of letters and their counts
     */
    @Operation(
        summary = "Get user's letter collection",
        description = "Retrieve all letters collected by the user and their current counts."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved letter collection",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LetterCollectionDTO.class),
                examples = @ExampleObject(
                    name = "Letter collection",
                    value = """
                    [
                      {
                        "letter": "H",
                        "count": 2
                      },
                      {
                        "letter": "A",
                        "count": 1
                      },
                      {
                        "letter": "P",
                        "count": 3
                      }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @GetMapping("/collection")
    public ResponseEntity<List<LetterCollectionDTO>> getUserLetterCollection(
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        List<LetterCollectionDTO> collection = letterService.getUserLetterCollection(userId);
        return ResponseEntity.ok(collection);
    }
    
    /**
     * Get available words for collection bonuses
     * 
     * @param userId The user ID from request header
     * @return List of available words with claim status
     */
    @GetMapping("/words")
    public ResponseEntity<List<LetterWordDTO>> getAvailableWords(
            @Parameter(description = "User ID (optional) - if provided, includes user-specific progress",
                    required = false, example = "12345")
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        List<LetterWordDTO> words = letterService.getAvailableWords(userId);
        return ResponseEntity.ok(words);
    }
    
    /**
     * Get words that user can currently claim
     * 
     * @param userId The user ID from request header
     * @return List of claimable words
     */
    @GetMapping("/words/claimable")
    public ResponseEntity<List<LetterWordDTO>> getClaimableWords(
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        List<LetterWordDTO> claimableWords = letterService.getClaimableWords(userId);
        return ResponseEntity.ok(claimableWords);
    }
    
    /**
     * Claim word bonus
     * 
     * @param wordId The word ID to claim
     * @param userId The user ID from request header
     * @return Success response
     */
    @Operation(
        summary = "Claim word bonus",
        description = """
        Claim cash bonus for completing a letter word. The user must have collected all required letters.
        
        **Parameters:**
        - Word ID: Provided in URL path (e.g., /letters/words/1/claim)
        - User ID: Provided in X-User-Id header
        
        **No request body is required for this endpoint.**
        """
    )
    @PostMapping("/words/{wordId}/claim")
    public ResponseEntity<Map<String, Object>> claimWordBonus(
            @Parameter(
                description = "Word ID to claim (provided in URL path)", 
                required = true, 
                example = "1"
            )
            @PathVariable @NotNull @Positive Long wordId,
            @Parameter(
                description = "User ID (provided in request header)", 
                required = true, 
                example = "12345"
            )
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        try {
            letterService.claimWordBonus(userId, wordId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Word bonus claimed successfully",
                "wordId", wordId
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw e; // Let global exception handler deal with it
        } catch (IllegalStateException e) {
            throw e; // Let global exception handler deal with it
        }
    }
    
    /**
     * Check if user can claim a specific word
     * 
     * @param wordId The word ID to check
     * @param userId The user ID from request header
     * @return Claim eligibility status
     */
    @GetMapping("/words/{wordId}/eligibility")
    public ResponseEntity<Map<String, Object>> checkWordEligibility(
            @PathVariable @NotNull @Positive Long wordId,
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        boolean canClaim = letterService.canClaimWord(userId, wordId);
        
        Map<String, Object> response = Map.of(
            "wordId", wordId,
            "canClaim", canClaim
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user's letter collection statistics
     * 
     * @param userId The user ID from request header
     * @return Collection statistics
     */
    @GetMapping("/collection/stats")
    public ResponseEntity<Map<String, Object>> getCollectionStatistics(
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        Long totalLetterCount = letterService.getTotalLetterCount(userId);
        long distinctLetterCount = letterService.getDistinctLetterCount(userId);
        List<String> distinctLetters = letterService.getDistinctLetters(userId);
        boolean hasClaimableWords = letterService.hasClaimableWords(userId);
        
        Map<String, Object> response = Map.of(
            "totalLetterCount", totalLetterCount,
            "distinctLetterCount", distinctLetterCount,
            "distinctLetters", distinctLetters,
            "hasClaimableWords", hasClaimableWords
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get count of a specific letter for user
     * 
     * @param letter The letter to check
     * @param userId The user ID from request header
     * @return Letter count
     */
    @GetMapping("/collection/{letter}")
    public ResponseEntity<Map<String, Object>> getLetterCount(
            @PathVariable @NotNull String letter,
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        if (letter.length() != 1 || !Character.isLetter(letter.charAt(0))) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "INVALID_LETTER",
                "message", "Letter must be a single alphabetic character"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Integer count = letterService.getLetterCount(userId, letter);
        
        Map<String, Object> response = Map.of(
            "letter", letter.toUpperCase(),
            "count", count
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if user has at least the required amount of a specific letter
     * 
     * @param letter The letter to check
     * @param requiredCount The required count
     * @param userId The user ID from request header
     * @return Whether user has sufficient letters
     */
    @GetMapping("/collection/{letter}/check/{requiredCount}")
    public ResponseEntity<Map<String, Object>> checkLetterRequirement(
            @PathVariable @NotNull String letter,
            @PathVariable @NotNull @Positive Integer requiredCount,
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        if (letter.length() != 1 || !Character.isLetter(letter.charAt(0))) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "INVALID_LETTER",
                "message", "Letter must be a single alphabetic character"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        boolean hasSufficient = letterService.hasAtLeastLetters(userId, letter, requiredCount);
        Integer actualCount = letterService.getLetterCount(userId, letter);
        
        Map<String, Object> response = Map.of(
            "letter", letter.toUpperCase(),
            "requiredCount", requiredCount,
            "actualCount", actualCount,
            "hasSufficient", hasSufficient
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all available words (without user-specific claim status)
     * 
     * @return List of all available words
     */
    @GetMapping("/words/all")
    public ResponseEntity<List<LetterWordDTO>> getAllAvailableWords() {
        List<LetterWordDTO> words = letterService.getAllAvailableWords();
        return ResponseEntity.ok(words);
    }
}