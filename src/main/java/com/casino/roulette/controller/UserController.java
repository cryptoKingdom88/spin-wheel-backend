package com.casino.roulette.controller;

import com.casino.roulette.entity.User;
import com.casino.roulette.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for user-related operations
 */
@RestController
@RequestMapping("/users")
@CrossOrigin(originPatterns = "*", maxAge = 3600, allowCredentials = "false")
@Validated
@Tag(name = "Users", description = "User information and status operations")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Get user's available spins count
     * 
     * @param userId The user ID from request header
     * @return User's available spins information
     */
    @Operation(
        summary = "Get user's available spins",
        description = "Retrieve the current number of free spins available for the user."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user's available spins",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Available spins",
                    value = """
                    {
                      "userId": 123,
                      "availableSpins": 5,
                      "canSpin": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/spins")
    public ResponseEntity<Map<String, Object>> getAvailableSpins(
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        User user = userService.validateUserExists(userId);
        
        Map<String, Object> response = Map.of(
            "userId", userId,
            "availableSpins", user.getAvailableSpins(),
            "canSpin", user.getAvailableSpins() > 0
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user's basic information
     * 
     * @param userId The user ID from request header
     * @return User's basic information
     */
    @Operation(
        summary = "Get user's basic information",
        description = "Retrieve user's basic information including cash balance, spins, and status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user information",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "User information",
                    value = """
                    {
                      "userId": 123,
                      "cashBalance": 150.50,
                      "availableSpins": 3,
                      "firstDepositBonusUsed": true,
                      "lastDailyLogin": "2025-07-31T10:30:00",
                      "canSpin": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        User user = userService.validateUserExists(userId);
        
        Map<String, Object> response = Map.of(
            "userId", userId,
            "cashBalance", user.getCashBalance(),
            "availableSpins", user.getAvailableSpins(),
            "firstDepositBonusUsed", user.getFirstDepositBonusUsed(),
            "lastDailyLogin", user.getLastDailyLogin(),
            "canSpin", user.getAvailableSpins() > 0
        );
        
        return ResponseEntity.ok(response);
    }
}