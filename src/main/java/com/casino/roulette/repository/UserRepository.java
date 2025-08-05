package com.casino.roulette.repository;

import com.casino.roulette.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by ID, creating if not exists
     */
    Optional<User> findById(Long id);
    
    /**
     * Update user's cash balance
     */
    @Modifying
    @Query("UPDATE User u SET u.cashBalance = u.cashBalance + :amount WHERE u.id = :userId")
    int updateCashBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    /**
     * Update user's available spins
     */
    @Modifying
    @Query("UPDATE User u SET u.availableSpins = u.availableSpins + :spins WHERE u.id = :userId")
    int updateAvailableSpins(@Param("userId") Long userId, @Param("spins") Integer spins);
    
    /**
     * Set user's last daily login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastDailyLogin = :loginTime WHERE u.id = :userId")
    int updateLastDailyLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * Mark first deposit bonus as used
     */
    @Modifying
    @Query("UPDATE User u SET u.firstDepositBonusUsed = true WHERE u.id = :userId")
    int markFirstDepositBonusUsed(@Param("userId") Long userId);
    
    /**
     * Check if user has sufficient spins
     */
    @Query("SELECT CASE WHEN u.availableSpins >= :requiredSpins THEN true ELSE false END FROM User u WHERE u.id = :userId")
    boolean hasSufficientSpins(@Param("userId") Long userId, @Param("requiredSpins") Integer requiredSpins);
    
    /**
     * Consume spins from user's balance
     */
    @Modifying
    @Query("UPDATE User u SET u.availableSpins = u.availableSpins - :spins WHERE u.id = :userId AND u.availableSpins >= :spins")
    int consumeSpins(@Param("userId") Long userId, @Param("spins") Integer spins);
    
    /**
     * Set user's last daily mission claim timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastDailyMissionClaim = :claimTime WHERE u.id = :userId")
    int updateLastDailyMissionClaim(@Param("userId") Long userId, @Param("claimTime") LocalDateTime claimTime);
}