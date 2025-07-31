package com.casino.roulette.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for letter word information
 */
public class LetterWordDTO {
    
    @NotNull
    private Long id;
    
    @NotBlank
    private String word;
    
    @NotNull
    @NotEmpty
    private Map<String, Integer> requiredLetters;
    
    @NotNull
    @Positive
    private BigDecimal rewardAmount;
    
    @NotNull
    private Boolean canClaim;
    
    // Default constructor
    public LetterWordDTO() {}
    
    // Full constructor
    public LetterWordDTO(Long id, String word, Map<String, Integer> requiredLetters, 
                        BigDecimal rewardAmount, Boolean canClaim) {
        this.id = id;
        this.word = word;
        this.requiredLetters = requiredLetters;
        this.rewardAmount = rewardAmount;
        this.canClaim = canClaim;
    }
    
    // Constructor without canClaim (defaults to false)
    public LetterWordDTO(Long id, String word, Map<String, Integer> requiredLetters, 
                        BigDecimal rewardAmount) {
        this.id = id;
        this.word = word;
        this.requiredLetters = requiredLetters;
        this.rewardAmount = rewardAmount;
        this.canClaim = false;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getWord() {
        return word;
    }
    
    public void setWord(String word) {
        this.word = word;
    }
    
    public Map<String, Integer> getRequiredLetters() {
        return requiredLetters;
    }
    
    public void setRequiredLetters(Map<String, Integer> requiredLetters) {
        this.requiredLetters = requiredLetters;
    }
    
    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }
    
    public void setRewardAmount(BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
    }
    
    public Boolean getCanClaim() {
        return canClaim;
    }
    
    public void setCanClaim(Boolean canClaim) {
        this.canClaim = canClaim;
    }
    
    /**
     * Get the total number of letters required for this word
     */
    public int getTotalLettersRequired() {
        if (requiredLetters == null) {
            return 0;
        }
        return requiredLetters.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get the number of unique letters required for this word
     */
    public int getUniqueLettersRequired() {
        if (requiredLetters == null) {
            return 0;
        }
        return requiredLetters.size();
    }
    
    /**
     * Check if a specific letter is required for this word
     */
    public boolean requiresLetter(String letter) {
        if (requiredLetters == null || letter == null) {
            return false;
        }
        return requiredLetters.containsKey(letter.toUpperCase());
    }
    
    /**
     * Get the required count for a specific letter
     */
    public int getRequiredCount(String letter) {
        if (requiredLetters == null || letter == null) {
            return 0;
        }
        return requiredLetters.getOrDefault(letter.toUpperCase(), 0);
    }
    
    @Override
    public String toString() {
        return "LetterWordDTO{" +
                "id=" + id +
                ", word='" + word + '\'' +
                ", requiredLetters=" + requiredLetters +
                ", rewardAmount=" + rewardAmount +
                ", canClaim=" + canClaim +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LetterWordDTO that = (LetterWordDTO) o;
        
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}