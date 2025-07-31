package com.casino.roulette.repository;

import com.casino.roulette.entity.DailyLoginMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLoginMissionRepository extends JpaRepository<DailyLoginMission, Long> {
    
    /**
     * Find all active daily login missions
     */
    List<DailyLoginMission> findByActiveTrue();
    
    /**
     * Find the first active daily login mission (there should typically be only one)
     */
    Optional<DailyLoginMission> findFirstByActiveTrue();
    
    /**
     * Check if there are any active daily login missions
     */
    boolean existsByActiveTrue();
    
    /**
     * Get the default daily login mission configuration
     */
    @Query("SELECT dlm FROM DailyLoginMission dlm WHERE dlm.active = true ORDER BY dlm.id ASC LIMIT 1")
    Optional<DailyLoginMission> getDefaultDailyLoginMission();
}