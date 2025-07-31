package com.casino.roulette.repository;

import com.casino.roulette.entity.DepositMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepositMissionRepository extends JpaRepository<DepositMission, Long> {
    
    /**
     * Find all active deposit missions
     */
    List<DepositMission> findByActiveTrue();
    
    /**
     * Find active missions that match the deposit amount range
     */
    @Query("SELECT dm FROM DepositMission dm WHERE dm.active = true " +
           "AND dm.minAmount <= :amount " +
           "AND (dm.maxAmount IS NULL OR dm.maxAmount >= :amount)")
    List<DepositMission> findActiveByAmountRange(@Param("amount") BigDecimal amount);
    
    /**
     * Find specific mission by amount range for exact matching
     */
    @Query("SELECT dm FROM DepositMission dm WHERE dm.active = true " +
           "AND dm.minAmount = :minAmount " +
           "AND (dm.maxAmount = :maxAmount OR (dm.maxAmount IS NULL AND :maxAmount IS NULL))")
    Optional<DepositMission> findByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                              @Param("maxAmount") BigDecimal maxAmount);
    
    /**
     * Find missions ordered by minimum amount (for tier display)
     */
    @Query("SELECT dm FROM DepositMission dm WHERE dm.active = true ORDER BY dm.minAmount ASC")
    List<DepositMission> findActiveOrderedByMinAmount();
    
    /**
     * Check if a mission exists for a specific amount range
     */
    @Query("SELECT COUNT(dm) > 0 FROM DepositMission dm WHERE dm.active = true " +
           "AND dm.minAmount <= :amount " +
           "AND (dm.maxAmount IS NULL OR dm.maxAmount >= :amount)")
    boolean existsActiveForAmount(@Param("amount") BigDecimal amount);
    
    /**
     * Find mission by name (for configuration purposes)
     */
    Optional<DepositMission> findByNameAndActiveTrue(String name);
}