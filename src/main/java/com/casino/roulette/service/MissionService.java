package com.casino.roulette.service;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.entity.DepositMission;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.UserMissionProgress;
import com.casino.roulette.repository.DepositMissionRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserMissionProgressRepository;
import com.casino.roulette.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MissionService {
    
    private final DepositMissionRepository depositMissionRepository;
    private final UserMissionProgressRepository userMissionProgressRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    
    @Autowired
    public MissionService(DepositMissionRepository depositMissionRepository,
                         UserMissionProgressRepository userMissionProgressRepository,
                         TransactionLogRepository transactionLogRepository,
                         UserService userService) {
        this.depositMissionRepository = depositMissionRepository;
        this.userMissionProgressRepository = userMissionProgressRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.userService = userService;
    }
    
    /**
     * Get available missions for a user
     */
    @Transactional(readOnly = true)
    public List<MissionDTO> getAvailableMissions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        List<DepositMission> activeMissions = depositMissionRepository.findActiveOrderedByMinAmount();
        List<MissionDTO> missionDTOs = new ArrayList<>();
        
        for (DepositMission mission : activeMissions) {
            Optional<UserMissionProgress> progressOpt = userMissionProgressRepository
                .findByUserIdAndMissionId(userId, mission.getId());
            
            Integer claimsUsed = progressOpt.map(UserMissionProgress::getClaimsUsed).orElse(0);
            boolean canClaim = claimsUsed < mission.getMaxClaims();
            
            String description = buildMissionDescription(mission);
            
            MissionDTO dto = new MissionDTO(
                mission.getId(),
                mission.getName(),
                description,
                mission.getSpinsGranted(),
                canClaim,
                claimsUsed,
                mission.getMaxClaims()
            );
            
            missionDTOs.add(dto);
        }
        
        return missionDTOs;
    }
    
    /**
     * Claim mission reward (spins)
     */
    public void claimMissionReward(Long userId, Long missionId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (missionId == null) {
            throw new IllegalArgumentException("Mission ID cannot be null");
        }
        
        // Ensure user exists
        userService.getOrCreateUser(userId);
        
        // Get mission
        DepositMission mission = depositMissionRepository.findById(missionId)
            .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionId));
        
        if (!mission.getActive()) {
            throw new IllegalStateException("Mission is not active: " + missionId);
        }
        
        // Get or create user progress
        UserMissionProgress progress = userMissionProgressRepository
            .findByUserIdAndMissionId(userId, missionId)
            .orElse(new UserMissionProgress(userId, missionId));
        
        // Check if user can still claim
        if (!progress.canClaim(mission.getMaxClaims())) {
            throw new IllegalStateException("User has reached maximum claims for mission: " + missionId);
        }
        
        // Increment claims and update timestamp
        progress.incrementClaims();
        userMissionProgressRepository.save(progress);
        
        // Grant spins to user
        userService.grantSpins(userId, mission.getSpinsGranted(), 
            "Mission reward: " + mission.getName());
        
        // Log the mission claim transaction
        TransactionLog log = TransactionLog.createDepositMissionSpinLog(
            userId, mission.getName(), mission.getSpinsGranted());
        transactionLogRepository.save(log);
    }
    
    /**
     * Process deposit missions for a user's deposit
     */
    public void processDepositMissions(Long userId, BigDecimal depositAmount) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        // Ensure user exists
        userService.getOrCreateUser(userId);
        
        // Find missions that match the deposit amount
        List<DepositMission> eligibleMissions = depositMissionRepository
            .findActiveByAmountRange(depositAmount);
        
        for (DepositMission mission : eligibleMissions) {
            // Check if user is still eligible for this mission
            Optional<UserMissionProgress> progressOpt = userMissionProgressRepository
                .findByUserIdAndMissionId(userId, mission.getId());
            
            Integer claimsUsed = progressOpt.map(UserMissionProgress::getClaimsUsed).orElse(0);
            
            if (claimsUsed < mission.getMaxClaims()) {
                // User is eligible - create or update progress but don't auto-claim
                // The user needs to manually claim through the mission interface
                if (progressOpt.isEmpty()) {
                    // Create initial progress record
                    UserMissionProgress newProgress = new UserMissionProgress(userId, mission.getId());
                    userMissionProgressRepository.save(newProgress);
                }
            }
        }
        
        // Log the deposit transaction
        TransactionLog depositLog = TransactionLog.createDepositLog(userId, depositAmount);
        transactionLogRepository.save(depositLog);
        
        // Update user's cash balance
        userService.updateCashBalance(userId, depositAmount);
    }
    
    /**
     * Check if user is eligible for a specific mission
     */
    @Transactional(readOnly = true)
    public boolean isUserEligibleForMission(Long userId, Long missionId) {
        if (userId == null || missionId == null) {
            return false;
        }
        
        return userMissionProgressRepository.isUserEligibleForMission(userId, missionId);
    }
    
    /**
     * Get remaining claims for a user on a specific mission
     */
    @Transactional(readOnly = true)
    public Integer getRemainingClaims(Long userId, Long missionId) {
        if (userId == null || missionId == null) {
            return 0;
        }
        
        return userMissionProgressRepository.getRemainingClaimsForMission(userId, missionId);
    }
    
    /**
     * Get user's progress on all missions
     */
    @Transactional(readOnly = true)
    public List<UserMissionProgress> getUserMissionProgress(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return userMissionProgressRepository.findByUserIdWithMission(userId);
    }
    
    /**
     * Get missions that match a specific deposit amount
     */
    @Transactional(readOnly = true)
    public List<DepositMission> getMissionsForAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ArrayList<>();
        }
        
        return depositMissionRepository.findActiveByAmountRange(amount);
    }
    
    /**
     * Build mission description based on amount range and rewards
     */
    private String buildMissionDescription(DepositMission mission) {
        StringBuilder description = new StringBuilder();
        description.append("Deposit ");
        
        if (mission.getMaxAmount() != null) {
            description.append("$").append(mission.getMinAmount())
                      .append(" - $").append(mission.getMaxAmount());
        } else {
            description.append("$").append(mission.getMinAmount()).append(" or more");
        }
        
        description.append(" to earn ").append(mission.getSpinsGranted())
                  .append(" free spin").append(mission.getSpinsGranted() > 1 ? "s" : "")
                  .append(". Maximum ").append(mission.getMaxClaims())
                  .append(" claim").append(mission.getMaxClaims() > 1 ? "s" : "")
                  .append(" allowed.");
        
        return description.toString();
    }
    
    /**
     * Get total spins available for claiming by user across all missions
     */
    @Transactional(readOnly = true)
    public Integer getTotalAvailableSpins(Long userId) {
        if (userId == null) {
            return 0;
        }
        
        List<MissionDTO> missions = getAvailableMissions(userId);
        return missions.stream()
            .filter(MissionDTO::getCanClaim)
            .mapToInt(mission -> mission.getSpinsAvailable() * mission.getRemainingClaims())
            .sum();
    }
    
    /**
     * Check if user has any claimable missions
     */
    @Transactional(readOnly = true)
    public boolean hasClaimableMissions(Long userId) {
        if (userId == null) {
            return false;
        }
        
        List<MissionDTO> missions = getAvailableMissions(userId);
        return missions.stream().anyMatch(MissionDTO::getCanClaim);
    }
}