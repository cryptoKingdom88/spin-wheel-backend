package com.casino.roulette.controller;

import com.casino.roulette.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(originPatterns = "*", maxAge = 3600, allowCredentials = "false")
@Tag(name = "Authentication", description = "User authentication and login operations")
public class AuthController {
    
    private final UserService userService;
    
    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = """
        Process user login and grant daily login spin if eligible. This is a simplified login endpoint for testing purposes.
        
        **Parameters:**
        - User ID: Provided in X-User-Id header
        
        **No request body is required for this endpoint.**
        """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> login(
            @Parameter(
                description = "User ID for login (provided in request header)", 
                required = true, 
                example = "12345"
            )
            @RequestHeader("X-User-Id") Long userId) {
        
        try {
            // Just ensure user exists (don't auto-grant daily spin)
            userService.getOrCreateUser(userId);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("userId", userId);
            response.put("note", "Check missions to claim daily login bonus");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "INVALID_USER_ID");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/status")
    @Operation(
        summary = "Get user login status",
        description = "Get user's login status and daily spin eligibility"
    )
    public ResponseEntity<Map<String, Object>> getLoginStatus(
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") Long userId) {
        
        try {
            var user = userService.getUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("userExists", user != null);
            
            if (user != null) {
                response.put("lastDailyLogin", user.getLastDailyLogin());
                response.put("availableSpins", user.getAvailableSpins());
                response.put("cashBalance", user.getCashBalance());
                response.put("firstDepositBonusUsed", user.getFirstDepositBonusUsed());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}