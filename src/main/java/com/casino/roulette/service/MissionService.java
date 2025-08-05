package com.casino.roulette.service;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.entity.DailyLoginMission;
import com.casino.roulette.entity.DepositMission;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.entity.UserMissionProgress;
import com.casino.roulette.repository.DailyLoginMissionRepository;
import com.casino.roulette.repository.DepositMissionRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserMissionProgressRepository;
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
    private final DailyLoginMissionRepository dailyLoginMissionRepository;
    private final UserMissionProgressRepository userMissionProgressRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    
    @Autowired
    public MissionService(DepositMissionRepository depositMissionRepository,
                         DailyLoginMissionRepository dailyLoginMissionRepository,
                         UserMissionProgressRepository userMissionProgressRepository,
                         TransactionLogRepository transactionLogRepository,
                         UserService userService) {
        this.depositMissionRepository = depositMissionRepository;
        this.dailyLoginMissionRepository = dailyLoginMissionRepository;
        this.userMissionProgressRepository = userMissionProgressRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.userService = userService;
    }
    
    /**
     * Get available missions for a user (includes both deposit missions and daily login mission)
     */
    @Transactional(readOnly = true)
    public List<MissionDTO> getAvailableMissions(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // For mission list, we allow new users (they can claim daily login)
        // So we use getOrCreateUser instead of validateUserExists
        userService.getOrCreateUser(userId);
        
        List<MissionDTO> missionDTOs = new ArrayList<>();
        
        // Add daily login mission
        Optional<DailyLoginMission> dailyLoginMissionOpt = dailyLoginMissionRepository.findFirstByActiveTrue();
        if (dailyLoginMissionOpt.isPresent()) {
            DailyLoginMission dailyLoginMission = dailyLoginMissionOpt.get();
            User user = userService.getUser(userId);
            
            boolean canClaimDaily = canClaimDailyLogin(user);
            String dailyDescription = buildDailyLoginDescription(dailyLoginMission, user);
            
            // Check if user already claimed today
            int claimsUsedToday = canClaimDaily ? 0 : 1;
            
            MissionDTO dailyMissionDTO = new MissionDTO(
                -1L, // Special ID for daily login mission
                dailyLoginMission.getName(),
                dailyDescription,
                dailyLoginMission.getSpinsGranted(), // Spins per claim (always 1 for daily login)
                canClaimDaily ? dailyLoginMission.getSpinsGranted() : 0, // Pending spins (1 if can claim, 0 if already claimed)
                canClaimDaily,
                claimsUsedToday, // 0 if can claim, 1 if already claimed today
                1 // Daily limit is 1 per day
            );
            
            missionDTOs.add(dailyMissionDTO);
        }
        
        // Add deposit missions
        List<DepositMission> activeMissions = depositMissionRepository.findActiveOrderedByMinAmount();
        for (DepositMission mission : activeMissions) {
            Optional<UserMissionProgress> progressOpt = userMissionProgressRepository
                .findByUserIdAndMissionId(userId, mission.getId());
            
            Integer claimsUsed = progressOpt.map(UserMissionProgress::getClaimsUsed).orElse(0);
            Integer availableClaims = progressOpt.map(UserMissionProgress::getAvailableClaims).orElse(0);
            boolean canClaim = availableClaims > 0 && claimsUsed < mission.getMaxClaims();
            
            String description = buildDepositMissionDescription(mission);
            
            MissionDTO dto = new MissionDTO(
                mission.getId(),
                mission.getName(),
                description,
                mission.getSpinsGranted(), // Spins per claim (e.g., 2 for $500 deposit)
                mission.getSpinsGranted() * availableClaims, // Total pending spins to claim
                canClaim,
                claimsUsed,
                mission.getMaxClaims()
            );
            
            missionDTOs.add(dto);
        }
        
        return missionDTOs;
    }
    
    /**
     * Get basic mission list without user-specific progress
     */
    @Transactional(readOnly = true)
    public List<MissionDTO> getBasicMissionList() {
        List<MissionDTO> missionDTOs = new ArrayList<>();
        
        // Add daily login mission (basic info only)
        Optional<DailyLoginMission> dailyLoginMissionOpt = dailyLoginMissionRepository.findFirstByActiveTrue();
        if (dailyLoginMissionOpt.isPresent()) {
            DailyLoginMission dailyLoginMission = dailyLoginMissionOpt.get();
            
            MissionDTO dailyMissionDTO = new MissionDTO(
                -1L, // Special ID for daily login mission
                dailyLoginMission.getName(),
                dailyLoginMission.getDescription(),
                dailyLoginMission.getSpinsGranted(), // Spins per claim
                0, // No pending spins without user context
                false, // Cannot determine claim status without user
                0, // No user-specific data
                1 // Daily limit is 1 per day
            );
            
            missionDTOs.add(dailyMissionDTO);
        }
        
        // Add deposit missions (basic info only)
        List<DepositMission> activeMissions = depositMissionRepository.findActiveOrderedByMinAmount();
        for (DepositMission mission : activeMissions) {
            String description = buildDepositMissionDescription(mission);
            
            MissionDTO dto = new MissionDTO(
                mission.getId(),
                mission.getName(),
                description,
                mission.getSpinsGranted(), // Spins per claim
                0, // No pending spins without user context
                false, // Cannot determine claim status without user
                0, // No user-specific data
                mission.getMaxClaims()
            );
            
            missionDTOs.add(dto);
        }
        
        return missionDTOs;
    }
    
    /**
     * Claim mission reward (spins) - handles both deposit missions and daily login mission
     */
    public void claimMissionReward(Long userId, Long missionId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (missionId == null) {
            throw new IllegalArgumentException("Mission ID cannot be null");
        }
        
        // Validate user exists first
        userService.validateUserExists(userId);
        
        // Check if this is a daily login mission (special ID -1)
        if (missionId == -1L) {
            claimDailyLoginMission(userId);
            return;
        }
        
        // Handle deposit mission
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
            throw new IllegalStateException("No available claims for mission: " + missionId);
        }
        
        // Claim all available rewards at once
        Integer claimedCount = progress.claimAllAvailable();
        userMissionProgressRepository.save(progress);
        
        // Grant spins to user (all accumulated spins at once)
        Integer totalSpins = mission.getSpinsGranted() * claimedCount;
        userService.grantSpins(userId, totalSpins, 
            "Mission reward: " + mission.getName() + " (x" + claimedCount + ")");
        
        // Log the mission claim transaction
        TransactionLog log = TransactionLog.createDepositMissionSpinLog(
            userId, mission.getName() + " (x" + claimedCount + ")", totalSpins);
        transactionLogRepository.save(log);
    }
    
    /**
     * Claim one mission reward (single claim)
     */
    public void claimOneMissionReward(Long userId, Long missionId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (missionId == null) {
            throw new IllegalArgumentException("Mission ID cannot be null");
        }
        
        // Validate user exists first
        userService.validateUserExists(userId);
        
        // Check if this is a daily login mission (special ID -1)
        if (missionId == -1L) {
            claimDailyLoginMission(userId);
            return;
        }
        
        // Handle deposit mission - claim only one
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
            throw new IllegalStateException("No available claims for mission: " + missionId);
        }
        
        // Claim only one reward
        Integer claimedCount = progress.claimOne();
        if (claimedCount == 0) {
            throw new IllegalStateException("No available claims for mission: " + missionId);
        }
        
        userMissionProgressRepository.save(progress);
        
        // Grant spins to user (single claim)
        Integer totalSpins = mission.getSpinsGranted() * claimedCount;
        userService.grantSpins(userId, totalSpins, 
            "Mission reward: " + mission.getName());
        
        // Log the mission claim transaction
        TransactionLog log = TransactionLog.createDepositMissionSpinLog(
            userId, mission.getName(), totalSpins);
        transactionLogRepository.save(log);
    }
    
    /**
     * Claim daily login mission reward
     */
    private void claimDailyLoginMission(Long userId) {
        // Use the dedicated daily mission spin method
        boolean spinGranted = userService.grantDailyMissionSpin(userId);
        
        if (!spinGranted) {
            throw new IllegalStateException("Daily mission spin already claimed today");
        }
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
        
        // Validate user exists first
        userService.validateUserExists(userId);
        
        // Find missions that match the deposit amount
        List<DepositMission> eligibleMissions = depositMissionRepository
            .findActiveByAmountRange(depositAmount);
        
        for (DepositMission mission : eligibleMissions) {
            // Get or create user progress
            UserMissionProgress progress = userMissionProgressRepository
                .findByUserIdAndMissionId(userId, mission.getId())
                .orElse(new UserMissionProgress(userId, mission.getId()));
            
            // Check if user can accumulate more claims
            if (progress.canAccumulateMore(mission.getMaxClaims())) {
                // Add one available claim for this deposit
                progress.addAvailableClaims(1);
                userMissionProgressRepository.save(progress);
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
        
        // Special handling for Daily Login Mission
        if (missionId == -1L) {
            User user = userService.getUser(userId);
            return canClaimDailyLogin(user);
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
        
        // Special handling for Daily Login Mission
        if (missionId == -1L) {
            User user = userService.getUser(userId);
            return canClaimDailyLogin(user) ? 1 : 0;
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
     * Check if user can claim daily login bonus
     */
    private boolean canClaimDailyLogin(User user) {
        if (user == null) {
            return true; // New user can claim
        }
        
        if (user.getLastDailyMissionClaim() == null) {
            return true; // Never claimed daily mission before
        }
        
        // Check if last daily mission claim was on a different day
        return !user.getLastDailyMissionClaim().toLocalDate().equals(java.time.LocalDate.now());
    }
    
    /**
     * Build daily login mission description
     */
    private String buildDailyLoginDescription(DailyLoginMission mission, User user) {
        StringBuilder description = new StringBuilder();
        description.append(mission.getDescription());
        
        if (user != null && user.getLastDailyMissionClaim() != null) {
            if (user.getLastDailyMissionClaim().toLocalDate().equals(java.time.LocalDate.now())) {
                description.append(" (Already claimed today)");
            } else {
                description.append(" (Available now!)");
            }
        } else {
            description.append(" (Available now!)");
        }
        
        return description.toString();
    }
    
    /**
     * Build deposit mission description based on amount range and rewards
     */
    private String buildDepositMissionDescription(DepositMission mission) {
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
            .mapToInt(MissionDTO::getPendingSpins)
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