package com.casino.roulette.repository;

import com.casino.roulette.entity.RouletteSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouletteSlotRepository extends JpaRepository<RouletteSlot, Long> {
    
    /**
     * Find all active roulette slots
     */
    List<RouletteSlot> findByActiveTrue();
    
    /**
     * Find all active slots ordered by weight descending for weighted selection
     */
    @Query("SELECT rs FROM RouletteSlot rs WHERE rs.active = true ORDER BY rs.weight DESC")
    List<RouletteSlot> findActiveSlotsByWeightDesc();
    
    /**
     * Find active slots by slot type
     */
    List<RouletteSlot> findByActiveTrueAndSlotType(RouletteSlot.SlotType slotType);
    
    /**
     * Get the sum of all weights for active slots (used for weighted random selection)
     */
    @Query("SELECT COALESCE(SUM(rs.weight), 0) FROM RouletteSlot rs WHERE rs.active = true")
    Long getTotalWeightForActiveSlots();
    
    /**
     * Find active slots with weight greater than or equal to specified value
     */
    @Query("SELECT rs FROM RouletteSlot rs WHERE rs.active = true AND rs.weight >= :minWeight ORDER BY rs.weight DESC")
    List<RouletteSlot> findActiveSlotsWithMinWeight(Integer minWeight);
    
    /**
     * Check if any active slots exist
     */
    boolean existsByActiveTrue();
    
    /**
     * Count active slots
     */
    long countByActiveTrue();
    
    /**
     * Count active slots by type
     */
    long countByActiveTrueAndSlotType(RouletteSlot.SlotType slotType);
}