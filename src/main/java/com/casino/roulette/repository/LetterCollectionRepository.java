package com.casino.roulette.repository;

import com.casino.roulette.entity.LetterCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LetterCollectionRepository extends JpaRepository<LetterCollection, Long> {
    
    /**
     * Find all letter collections for a specific user
     */
    List<LetterCollection> findByUserId(Long userId);
    
    /**
     * Find all letter collections for a user with count greater than 0
     */
    @Query("SELECT lc FROM LetterCollection lc WHERE lc.userId = :userId AND lc.count > 0")
    List<LetterCollection> findByUserIdWithPositiveCount(@Param("userId") Long userId);
    
    /**
     * Find a specific letter collection for a user and letter
     */
    Optional<LetterCollection> findByUserIdAndLetter(Long userId, String letter);
    
    /**
     * Find letter collections for a user with specific letters
     */
    List<LetterCollection> findByUserIdAndLetterIn(Long userId, List<String> letters);
    
    /**
     * Get the count of a specific letter for a user
     */
    @Query("SELECT COALESCE(MAX(lc.count), 0) FROM LetterCollection lc WHERE lc.userId = :userId AND lc.letter = :letter")
    Integer getLetterCountForUser(@Param("userId") Long userId, @Param("letter") String letter);
    
    /**
     * Check if user has at least the required amount of a specific letter
     */
    @Query("SELECT CASE WHEN COALESCE(MAX(lc.count), 0) >= :requiredCount THEN true ELSE false END " +
           "FROM LetterCollection lc WHERE lc.userId = :userId AND lc.letter = :letter")
    boolean hasAtLeastLetterCount(@Param("userId") Long userId, @Param("letter") String letter, @Param("requiredCount") Integer requiredCount);
    
    /**
     * Increment letter count for a user (updates existing record)
     */
    @Modifying
    @Query("UPDATE LetterCollection lc SET lc.count = lc.count + :amount WHERE lc.userId = :userId AND lc.letter = :letter")
    int incrementExistingLetterCount(@Param("userId") Long userId, @Param("letter") String letter, @Param("amount") Integer amount);
    
    /**
     * Decrement letter count for a user (ensures count doesn't go below 0)
     */
    @Modifying
    @Query("UPDATE LetterCollection lc SET lc.count = GREATEST(0, lc.count - :amount) " +
           "WHERE lc.userId = :userId AND lc.letter = :letter")
    int decrementLetterCount(@Param("userId") Long userId, @Param("letter") String letter, @Param("amount") Integer amount);
    
    /**
     * Update letter count for a user and letter
     */
    @Modifying
    @Query("UPDATE LetterCollection lc SET lc.count = :newCount WHERE lc.userId = :userId AND lc.letter = :letter")
    int updateLetterCount(@Param("userId") Long userId, @Param("letter") String letter, @Param("newCount") Integer newCount);
    
    /**
     * Delete letter collections with zero count for a user
     */
    @Modifying
    @Query("DELETE FROM LetterCollection lc WHERE lc.userId = :userId AND lc.count = 0")
    int deleteZeroCountLettersForUser(@Param("userId") Long userId);
    
    /**
     * Get total number of letters collected by a user
     */
    @Query("SELECT COALESCE(SUM(lc.count), 0) FROM LetterCollection lc WHERE lc.userId = :userId")
    Long getTotalLetterCountForUser(@Param("userId") Long userId);
    
    /**
     * Get distinct letters collected by a user
     */
    @Query("SELECT DISTINCT lc.letter FROM LetterCollection lc WHERE lc.userId = :userId AND lc.count > 0")
    List<String> getDistinctLettersForUser(@Param("userId") Long userId);
    
    /**
     * Check if user exists in letter collections
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Count distinct letters for a user
     */
    @Query("SELECT COUNT(DISTINCT lc.letter) FROM LetterCollection lc WHERE lc.userId = :userId AND lc.count > 0")
    long countDistinctLettersForUser(@Param("userId") Long userId);
}