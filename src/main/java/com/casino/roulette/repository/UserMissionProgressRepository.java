package com.casino.roulette.repository;

import com.casino.roulette.entity.UserMissionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMissionProgressRepository extends JpaRepository<UserMissionProgress, Long> {
    
    /**
     * Find user's progress for a specific mission
     */
    Optional<UserMissionProgress> findByUserIdAndMissionId(Long userId, Long missionId);
    
    /**
     * Find all progress records for a user
     */
    List<UserMissionProgress> findByUserId(Long userId);
    
    /**
     * Find all progress records for a mission
     */
    List<UserMissionProgress> findByMissionId(Long missionId);
    
    /**
     * Check if user can claim more rewards from a mission
     */
    @Query("SELECT CASE WHEN ump.claimsUsed < :maxClaims THEN true ELSE false END " +
           "FROM UserMissionProgress ump " +
           "WHERE ump.userId = :userId AND ump.missionId = :missionId")
    Optional<Boolean> canClaimMore(@Param("userId") Long userId, 
                                  @Param("missionId") Long missionId, 
                                  @Param("maxClaims") Integer maxClaims);
    
    /**
     * Get remaining claims for a user's mission
     */
    @Query("SELECT (:maxClaims - ump.claimsUsed) " +
           "FROM UserMissionProgress ump " +
           "WHERE ump.userId = :userId AND ump.missionId = :missionId")
    Optional<Integer> getRemainingClaims(@Param("userId") Long userId, 
                                        @Param("missionId") Long missionId, 
                                        @Param("maxClaims") Integer maxClaims);
    
    /**
     * Increment claims used for a user's mission
     */
    @Modifying
    @Query("UPDATE UserMissionProgress ump " +
           "SET ump.claimsUsed = ump.claimsUsed + 1, ump.lastClaimDate = :claimDate " +
           "WHERE ump.userId = :userId AND ump.missionId = :missionId")
    int incrementClaimsUsed(@Param("userId") Long userId, 
                           @Param("missionId") Long missionId, 
                           @Param("claimDate") LocalDateTime claimDate);
    
    /**
     * Check if user has reached maximum claims for a mission
     */
    @Query("SELECT CASE WHEN ump.claimsUsed >= :maxClaims THEN true ELSE false END " +
           "FROM UserMissionProgress ump " +
           "WHERE ump.userId = :userId AND ump.missionId = :missionId")
    Optional<Boolean> hasReachedMaxClaims(@Param("userId") Long userId, 
                                         @Param("missionId") Long missionId, 
                                         @Param("maxClaims") Integer maxClaims);
    
    /**
     * Find users who have not reached max claims for a mission
     */
    @Query("SELECT ump FROM UserMissionProgress ump " +
           "WHERE ump.missionId = :missionId AND ump.claimsUsed < :maxClaims")
    List<UserMissionProgress> findUsersWithRemainingClaims(@Param("missionId") Long missionId, 
                                                           @Param("maxClaims") Integer maxClaims);
    
    /**
     * Get total claims used across all users for a mission
     */
    @Query("SELECT COALESCE(SUM(ump.claimsUsed), 0) FROM UserMissionProgress ump WHERE ump.missionId = :missionId")
    Long getTotalClaimsForMission(@Param("missionId") Long missionId);
    
    /**
     * Delete progress records for inactive missions (cleanup)
     */
    @Modifying
    @Query("DELETE FROM UserMissionProgress ump WHERE ump.missionId IN " +
           "(SELECT dm.id FROM DepositMission dm WHERE dm.active = false)")
    int deleteProgressForInactiveMissions();
    
    /**
     * Check if user is eligible for a mission (hasn't reached max claims)
     * Requirements: 3.6, 3.7, 3.8, 3.9
     */
    @Query("SELECT CASE WHEN ump.id IS NULL THEN true " +
           "WHEN ump.claimsUsed < dm.maxClaims THEN true " +
           "ELSE false END " +
           "FROM DepositMission dm " +
           "LEFT JOIN UserMissionProgress ump ON ump.missionId = dm.id AND ump.userId = :userId " +
           "WHERE dm.id = :missionId AND dm.active = true")
    boolean isUserEligibleForMission(@Param("userId") Long userId, @Param("missionId") Long missionId);
    
    /**
     * Get remaining claims for a user on a specific mission
     * Requirements: 3.6, 3.7, 3.8, 3.9
     */
    @Query("SELECT CASE WHEN ump.id IS NULL THEN dm.maxClaims " +
           "ELSE GREATEST(0, dm.maxClaims - ump.claimsUsed) END " +
           "FROM DepositMission dm " +
           "LEFT JOIN UserMissionProgress ump ON ump.missionId = dm.id AND ump.userId = :userId " +
           "WHERE dm.id = :missionId")
    Integer getRemainingClaimsForMission(@Param("userId") Long userId, @Param("missionId") Long missionId);
    
    /**
     * Find users who have claimed from a mission within a date range
     * Requirements: 4.1.5
     */
    @Query("SELECT ump FROM UserMissionProgress ump " +
           "WHERE ump.missionId = :missionId " +
           "AND ump.lastClaimDate BETWEEN :startDate AND :endDate")
    List<UserMissionProgress> findByMissionIdAndClaimDateBetween(@Param("missionId") Long missionId,
                                                                @Param("startDate") LocalDateTime startDate,
                                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get total claims used by a user across all missions
     * Requirements: 3.6, 3.7, 3.8, 3.9
     */
    @Query("SELECT COALESCE(SUM(ump.claimsUsed), 0) FROM UserMissionProgress ump WHERE ump.userId = :userId")
    Long getTotalClaimsUsedByUser(@Param("userId") Long userId);
    
    /**
     * Find users with progress on active missions
     * Requirements: 4.1.5
     */
    @Query("SELECT DISTINCT ump FROM UserMissionProgress ump " +
           "JOIN ump.mission dm " +
           "WHERE dm.active = true AND ump.claimsUsed > 0")
    List<UserMissionProgress> findUsersWithActiveProgress();
    
    /**
     * Check if user has any claims on any mission
     * Requirements: 3.6, 3.7, 3.8, 3.9
     */
    boolean existsByUserIdAndClaimsUsedGreaterThan(Long userId, Integer claims);
    
    /**
     * Find progress records that can still be claimed (haven't reached max)
     * Requirements: 3.6, 3.7, 3.8, 3.9
     */
    @Query("SELECT ump FROM UserMissionProgress ump " +
           "JOIN ump.mission dm " +
           "WHERE ump.userId = :userId " +
           "AND dm.active = true " +
           "AND ump.claimsUsed < dm.maxClaims")
    List<UserMissionProgress> findEligibleProgressForUser(@Param("userId") Long userId);
    
    /**
     * Find progress with mission details for eligibility checking
     * Requirements: 3.6, 3.7, 3.8, 3.9
     */
    @Query("SELECT ump FROM UserMissionProgress ump " +
           "JOIN FETCH ump.mission " +
           "WHERE ump.userId = :userId AND ump.mission.active = true")
    List<UserMissionProgress> findByUserIdWithMission(@Param("userId") Long userId);
}