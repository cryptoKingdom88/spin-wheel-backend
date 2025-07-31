package com.casino.roulette.repository;

import com.casino.roulette.entity.LetterWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LetterWordRepository extends JpaRepository<LetterWord, Long> {
    
    /**
     * Find all active letter words
     */
    List<LetterWord> findByActiveTrue();
    
    /**
     * Find all active letter words ordered by reward amount descending
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true ORDER BY lw.rewardAmount DESC")
    List<LetterWord> findActiveWordsOrderByRewardDesc();
    
    /**
     * Find all active letter words ordered by reward amount ascending
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true ORDER BY lw.rewardAmount ASC")
    List<LetterWord> findActiveWordsOrderByRewardAsc();
    
    /**
     * Find active letter word by word text
     */
    Optional<LetterWord> findByWordAndActiveTrue(String word);
    
    /**
     * Find active letter words with reward amount greater than or equal to specified amount
     */
    List<LetterWord> findByActiveTrueAndRewardAmountGreaterThanEqual(BigDecimal minRewardAmount);
    
    /**
     * Find active letter words with reward amount less than or equal to specified amount
     */
    List<LetterWord> findByActiveTrueAndRewardAmountLessThanEqual(BigDecimal maxRewardAmount);
    
    /**
     * Find active letter words with reward amount between specified range
     */
    List<LetterWord> findByActiveTrueAndRewardAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);
    
    /**
     * Check if a word exists and is active
     */
    boolean existsByWordAndActiveTrue(String word);
    
    /**
     * Count active letter words
     */
    long countByActiveTrue();
    
    /**
     * Get the maximum reward amount among active words
     */
    @Query("SELECT MAX(lw.rewardAmount) FROM LetterWord lw WHERE lw.active = true")
    BigDecimal getMaxRewardAmount();
    
    /**
     * Get the minimum reward amount among active words
     */
    @Query("SELECT MIN(lw.rewardAmount) FROM LetterWord lw WHERE lw.active = true")
    BigDecimal getMinRewardAmount();
    
    /**
     * Get the average reward amount among active words
     */
    @Query("SELECT AVG(lw.rewardAmount) FROM LetterWord lw WHERE lw.active = true")
    BigDecimal getAverageRewardAmount();
    
    /**
     * Get the total reward amount of all active words
     */
    @Query("SELECT SUM(lw.rewardAmount) FROM LetterWord lw WHERE lw.active = true")
    BigDecimal getTotalRewardAmount();
    
    /**
     * Find active words containing specific letter in their required letters JSON
     * Note: This is a simple text search in JSON, not a proper JSON query
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true AND lw.requiredLetters LIKE %:letter%")
    List<LetterWord> findActiveWordsContainingLetter(@Param("letter") String letter);
    
    /**
     * Find words by partial word match (case insensitive)
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true AND UPPER(lw.word) LIKE UPPER(CONCAT('%', :partialWord, '%'))")
    List<LetterWord> findActiveWordsByPartialMatch(@Param("partialWord") String partialWord);
    
    /**
     * Find words that start with specific prefix
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true AND UPPER(lw.word) LIKE UPPER(CONCAT(:prefix, '%'))")
    List<LetterWord> findActiveWordsStartingWith(@Param("prefix") String prefix);
    
    /**
     * Find words by exact length
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true AND LENGTH(lw.word) = :length")
    List<LetterWord> findActiveWordsByLength(@Param("length") int length);
    
    /**
     * Find words with length between specified range
     */
    @Query("SELECT lw FROM LetterWord lw WHERE lw.active = true AND LENGTH(lw.word) BETWEEN :minLength AND :maxLength")
    List<LetterWord> findActiveWordsByLengthRange(@Param("minLength") int minLength, @Param("maxLength") int maxLength);
}