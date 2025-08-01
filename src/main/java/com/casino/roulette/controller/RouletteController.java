package com.casino.roulette.controller;

import com.casino.roulette.dto.SpinResultDTO;
import com.casino.roulette.entity.RouletteSlot;
import com.casino.roulette.service.RouletteService;
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
 * REST Controller for roulette-related operations
 */
@RestController
@RequestMapping("/roulette")
@CrossOrigin(originPatterns = "*", maxAge = 3600, allowCredentials = "false")
@Validated
@Tag(name = "Roulette", description = "Roulette game operations including spinning, slot configuration, and game status")
public class RouletteController {

    private final RouletteService rouletteService;

    @Autowired
    public RouletteController(RouletteService rouletteService) {
        this.rouletteService = rouletteService;
    }

    /**
     * Spin the roulette wheel
     * 
     * @param userId The user ID from request header
     * @return Spin result with winnings and remaining spins
     */
    @Operation(summary = "Spin the roulette wheel", description = """
            Consume one free spin to play the roulette game. Returns the result (cash or letter) and remaining spins.

            **Parameters:**
            - User ID: Provided in X-User-Id header

            **No request body is required for this endpoint.**
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Spin completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SpinResultDTO.class), examples = {
                    @ExampleObject(name = "Cash win", summary = "User wins cash", value = """
                            {
                              "type": "CASH",
                              "value": "5.00",
                              "cash": 5.00,
                              "letter": null,
                              "remainingSpins": 2
                            }
                            """),
                    @ExampleObject(name = "Letter win", summary = "User wins a letter", value = """
                            {
                              "type": "LETTER",
                              "value": "H",
                              "cash": null,
                              "letter": "H",
                              "remainingSpins": 2
                            }
                            """)
            })),
            @ApiResponse(responseCode = "400", description = "Invalid request - insufficient spins", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "No spins available", value = """
                    {
                      "success": false,
                      "error": "INSUFFICIENT_SPINS",
                      "message": "You don't have enough spins available",
                      "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """))),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping("/spin")
    public ResponseEntity<SpinResultDTO> spinRoulette(
            @Parameter(description = "User ID (provided in request header)", required = true, example = "12345") @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {

        try {
            SpinResultDTO result = rouletteService.spinRoulette(userId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            throw e; // Let global exception handler deal with it
        } catch (IllegalStateException e) {
            throw e; // Let global exception handler deal with it
        }
    }

    /**
     * Get current roulette slot configuration
     * 
     * @return List of active roulette slots
     */
    @Operation(summary = "Get roulette slot configuration", description = "Retrieve the current configuration of all active roulette slots including their types, values, and weights.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved slot configuration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RouletteSlot.class), examples = @ExampleObject(name = "Slot configuration", value = """
                    [
                      {
                        "id": 1,
                        "type": "CASH",
                        "value": "1.00",
                        "weight": 30,
                        "active": true
                      },
                      {
                        "id": 2,
                        "type": "LETTER",
                        "value": "H",
                        "weight": 20,
                        "active": true
                      }
                    ]
                    """))),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @GetMapping("/slots")
    public ResponseEntity<List<RouletteSlot>> getRouletteSlots() {
        List<RouletteSlot> slots = rouletteService.getRouletteConfiguration();
        return ResponseEntity.ok(slots);
    }

    /**
     * Update roulette slot configuration (admin endpoint)
     * 
     * @param slots List of roulette slots to update
     * @return Success response
     */
    @PutMapping("/slots")
    public ResponseEntity<Map<String, Object>> updateRouletteSlots(
            @RequestBody @NotNull List<RouletteSlot> slots) {

        try {
            rouletteService.updateRouletteSlots(slots);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Roulette slots updated successfully",
                    "slotsCount", slots.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw e; // Let global exception handler deal with it
        }
    }

    /**
     * Check if roulette is available for play
     * 
     * @return Availability status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRouletteStatus() {
        boolean isAvailable = rouletteService.isRouletteAvailable();
        Long totalWeight = rouletteService.getTotalActiveWeight();
        long cashSlotCount = rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.CASH);
        long letterSlotCount = rouletteService.getActiveSlotCountByType(RouletteSlot.SlotType.LETTER);

        Map<String, Object> response = Map.of(
                "available", isAvailable,
                "totalWeight", totalWeight != null ? totalWeight : 0L,
                "cashSlotCount", cashSlotCount,
                "letterSlotCount", letterSlotCount,
                "totalSlotCount", cashSlotCount + letterSlotCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Get roulette statistics by slot type
     * 
     * @param slotType The slot type to get statistics for (CASH or LETTER)
     * @return Statistics for the specified slot type
     */
    @GetMapping("/stats/{slotType}")
    public ResponseEntity<Map<String, Object>> getSlotTypeStatistics(
            @PathVariable @NotNull String slotType) {

        try {
            RouletteSlot.SlotType type = RouletteSlot.SlotType.valueOf(slotType.toUpperCase());
            long slotCount = rouletteService.getActiveSlotCountByType(type);

            Map<String, Object> response = Map.of(
                    "slotType", type.toString(),
                    "activeSlotCount", slotCount);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "error", "INVALID_SLOT_TYPE",
                    "message", "Slot type must be either CASH or LETTER",
                    "validTypes", List.of("CASH", "LETTER"));
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}