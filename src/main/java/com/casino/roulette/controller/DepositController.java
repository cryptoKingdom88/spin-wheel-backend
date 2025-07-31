package com.casino.roulette.controller;

import com.casino.roulette.entity.DepositMission;
import com.casino.roulette.service.MissionService;
import com.casino.roulette.service.TransactionService;
import com.casino.roulette.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for deposit processing and mission evaluation
 */
@RestController
@RequestMapping("/api/deposits")
@Validated
@Tag(name = "Deposits", description = "Deposit processing and mission evaluation operations")
public class DepositController {
    
    private final MissionService missionService;
    private final TransactionService transactionService;
    private final UserService userService;
    
    @Autowired
    public DepositController(MissionService missionService,
                           TransactionService transactionService,
                           UserService userService) {
        this.missionService = missionService;
        this.transactionService = transactionService;
        this.userService = userService;
    }
    
    /**
     * Process user deposit
     * 
     * @param depositRequest The deposit request containing amount
     * @param userId The user ID from request header
     * @return Deposit processing result with eligible missions
     */
    @Operation(
        summary = "Process user deposit",
        description = "Process a user deposit, update their balance, and evaluate eligible missions for free spins."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Deposit processed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Successful deposit",
                    value = """
                    {
                      "success": true,
                      "message": "Deposit processed successfully",
                      "depositAmount": 100.00,
                      "newBalance": 250.00,
                      "eligibleMissions": 2,
                      "missionIds": [1, 2]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid deposit amount",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid amount",
                    value = """
                    {
                      "success": false,
                      "error": "INVALID_DEPOSIT_AMOUNT",
                      "message": "Deposit amount must be greater than 0",
                      "timestamp": "2024-01-15T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping
    public ResponseEntity<Map<String, Object>> processDeposit(
            @Parameter(description = "Deposit request with amount", required = true)
            @RequestBody @Valid DepositRequest depositRequest,
            @Parameter(description = "User ID", required = true, example = "12345")
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        try {
            BigDecimal depositAmount = depositRequest.getAmount();
            
            // Get missions that match this deposit amount before processing
            List<DepositMission> eligibleMissions = missionService.getMissionsForAmount(depositAmount);
            
            // Process the deposit and evaluate missions
            missionService.processDepositMissions(userId, depositAmount);
            
            // Get user's updated balance
            BigDecimal newBalance = userService.getUser(userId).getCashBalance();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Deposit processed successfully",
                "depositAmount", depositAmount,
                "newBalance", newBalance,
                "eligibleMissions", eligibleMissions.size(),
                "missionIds", eligibleMissions.stream().map(DepositMission::getId).toList()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            throw e; // Let global exception handler deal with it
        }
    }
    
    /**
     * Get deposit history for user
     * 
     * @param userId The user ID from request header
     * @return List of deposit transactions
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getDepositHistory(
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        // Get deposit transactions from transaction service
        var depositTransactions = transactionService.getTransactionHistory(userId)
            .stream()
            .filter(log -> "DEPOSIT".equals(log.getTransactionType()))
            .toList();
        
        BigDecimal totalDeposited = depositTransactions.stream()
            .map(log -> log.getAmount())
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> response = Map.of(
            "deposits", depositTransactions,
            "totalDeposited", totalDeposited,
            "depositCount", depositTransactions.size()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get missions that would be eligible for a specific deposit amount
     * 
     * @param amount The deposit amount to check
     * @return List of eligible missions
     */
    @GetMapping("/missions/preview")
    public ResponseEntity<Map<String, Object>> previewEligibleMissions(
            @RequestParam @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount) {
        
        List<DepositMission> eligibleMissions = missionService.getMissionsForAmount(amount);
        
        Integer totalSpinsAvailable = eligibleMissions.stream()
            .mapToInt(DepositMission::getSpinsGranted)
            .sum();
        
        Map<String, Object> response = Map.of(
            "depositAmount", amount,
            "eligibleMissions", eligibleMissions,
            "missionCount", eligibleMissions.size(),
            "totalSpinsAvailable", totalSpinsAvailable
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if user has made their first deposit
     * 
     * @param userId The user ID from request header
     * @return First deposit status
     */
    @GetMapping("/first-deposit/status")
    public ResponseEntity<Map<String, Object>> getFirstDepositStatus(
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        var user = userService.getUser(userId);
        boolean hasFirstDepositBonus = user != null && user.getFirstDepositBonusUsed();
        
        Map<String, Object> response = Map.of(
            "hasUsedFirstDepositBonus", hasFirstDepositBonus,
            "isEligibleForFirstDepositBonus", !hasFirstDepositBonus
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user's total deposit statistics
     * 
     * @param userId The user ID from request header
     * @return Deposit statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDepositStatistics(
            @RequestHeader("X-User-Id") @NotNull @Positive Long userId) {
        
        var depositTransactions = transactionService.getTransactionHistory(userId)
            .stream()
            .filter(log -> "DEPOSIT".equals(log.getTransactionType()))
            .toList();
        
        BigDecimal totalDeposited = depositTransactions.stream()
            .map(log -> log.getAmount())
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageDeposit = depositTransactions.isEmpty() ? 
            BigDecimal.ZERO : 
            totalDeposited.divide(BigDecimal.valueOf(depositTransactions.size()), 2, java.math.RoundingMode.HALF_UP);
        
        var user = userService.getUser(userId);
        BigDecimal currentBalance = user != null ? user.getCashBalance() : BigDecimal.ZERO;
        
        Map<String, Object> response = Map.of(
            "totalDeposited", totalDeposited,
            "depositCount", depositTransactions.size(),
            "averageDeposit", averageDeposit,
            "currentBalance", currentBalance,
            "hasUsedFirstDepositBonus", user != null && user.getFirstDepositBonusUsed()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DTO for deposit requests
     */
    public static class DepositRequest {
        
        @NotNull(message = "Deposit amount is required")
        @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0")
        private BigDecimal amount;
        
        // Default constructor
        public DepositRequest() {}
        
        // Constructor
        public DepositRequest(BigDecimal amount) {
            this.amount = amount;
        }
        
        // Getter and setter
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
        
        @Override
        public String toString() {
            return "DepositRequest{" +
                    "amount=" + amount +
                    '}';
        }
    }
}