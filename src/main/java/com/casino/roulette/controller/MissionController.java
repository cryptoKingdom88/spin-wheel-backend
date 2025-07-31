package com.casino.roulette.controller;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.service.MissionService;
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
 * REST Controller for mission-related operations
 */
@RestController
@RequestMapping("/api/missions")
@Validated
@Tag(name = "Missions", description = "Mission management and reward claiming operations")
public class MissionController {
    
    private final MissionService missionService;
    
    @Autowired
    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }
    
    /**
     * Get available missions for a user
     * 
     * @param userId The user ID from request header or parameter
     * @return List of available missions
     */
    @Operation(
        summary = "Get available missions",
        description = "Retrieve all missions available for the specified user, including claim status and remaining claims"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved available missions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MissionDTO.class),
                examples = @ExampleObject(
                    name = "Available missions",
                    value = """
                    [
                      {
                        "id": 1,
                        "name": "Small Deposit Mission",
                        "description": "Deposit $50-$99 to earn 1 free spin",
                        "spinsAvailable": 1,
                        "canClaim": true,
                        "claimsUsed": 5,
                        "maxClaims": 50
                      }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<MissionDTO>> getAvailableMissions(
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        List<MissionDTO> missions = missionService.getAvailableMissions(userId);
        return ResponseEntity.ok(missions);
    }
    
    /**
     * Claim mission reward (spins)
     * 
     * @param missionId The mission ID to claim
     * @param userId The user ID from request header
     * @return Success response
     */
    @Operation(
        summary = "Claim mission reward",
        description = "Claim free spins from a completed mission. The mission must be eligible and the user must not have exceeded the maximum claims."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mission reward claimed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Success response",
                    value = """
                    {
                      "success": true,
                      "message": "Mission reward claimed successfully",
                      "missionId": 1
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or mission not available",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Error response",
                    value = """
                    {
                      "success": false,
                      "error": "MISSION_NOT_AVAILABLE",
                      "message": "Mission has reached maximum claims"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "422", description = "Mission not available for claiming"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{missionId}/claim")
    public ResponseEntity<Map<String, Object>> claimMissionReward(
            @Parameter(description = "Mission ID to claim", required = true, example = "1")
            @PathVariable @NotNull @Positive Long missionId,
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        try {
            missionService.claimMissionReward(userId, missionId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Mission reward claimed successfully",
                "missionId", missionId
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "INVALID_REQUEST",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (IllegalStateException e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "MISSION_NOT_AVAILABLE",
                "message", e.getMessage()
            );
            return ResponseEntity.unprocessableEntity().body(errorResponse);
        }
    }
    
    /**
     * Get user's mission progress
     * 
     * @param userId The user ID from request header
     * @return User's mission progress information
     */
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getMissionProgress(
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        List<MissionDTO> missions = missionService.getAvailableMissions(userId);
        Integer totalAvailableSpins = missionService.getTotalAvailableSpins(userId);
        boolean hasClaimableMissions = missionService.hasClaimableMissions(userId);
        
        Map<String, Object> response = Map.of(
            "missions", missions,
            "totalAvailableSpins", totalAvailableSpins,
            "hasClaimableMissions", hasClaimableMissions
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if user is eligible for a specific mission
     * 
     * @param missionId The mission ID to check
     * @param userId The user ID from request header
     * @return Eligibility status
     */
    @GetMapping("/{missionId}/eligibility")
    public ResponseEntity<Map<String, Object>> checkMissionEligibility(
            @PathVariable @NotNull @Positive Long missionId,
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        boolean isEligible = missionService.isUserEligibleForMission(userId, missionId);
        Integer remainingClaims = missionService.getRemainingClaims(userId, missionId);
        
        Map<String, Object> response = Map.of(
            "missionId", missionId,
            "isEligible", isEligible,
            "remainingClaims", remainingClaims
        );
        
        return ResponseEntity.ok(response);
    }
}