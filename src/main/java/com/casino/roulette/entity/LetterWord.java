package com.casino.roulette.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "letter_words")
public class LetterWord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "word", nullable = false, length = 50)
    @NotBlank(message = "Word cannot be blank")
    private String word;
    
    @Column(name = "required_letters", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Required letters cannot be blank")
    private String requiredLetters; // JSON format: {"H":1,"A":1,"P":2,"Y":1}
    
    @Column(name = "reward_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Reward amount cannot be null")
    @DecimalMin(value = "0.01", message = "Reward amount must be greater than 0")
    private BigDecimal rewardAmount;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Default constructor
    public LetterWord() {}
    
    // Constructor with required fields
    public LetterWord(String word, Map<String, Integer> requiredLettersMap, BigDecimal rewardAmount) {
        this.word = word != null ? word.toUpperCase() : null;
        this.setRequiredLettersMap(requiredLettersMap);
        this.rewardAmount = rewardAmount;
    }
    
    // Getters and Setters
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
        this.word = word != null ? word.toUpperCase() : null;
    }
    
    public String getRequiredLetters() {
        return requiredLetters;
    }
    
    public void setRequiredLetters(String requiredLetters) {
        this.requiredLetters = requiredLetters;
    }
    
    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }
    
    public void setRewardAmount(BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Gets the required letters as a Map
     */
    public Map<String, Integer> getRequiredLettersMap() {
        if (requiredLetters == null || requiredLetters.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(requiredLetters, new TypeReference<Map<String, Integer>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }
    
    /**
     * Sets the required letters from a Map
     */
    public void setRequiredLettersMap(Map<String, Integer> requiredLettersMap) {
        if (requiredLettersMap == null) {
            this.requiredLetters = "{}";
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.requiredLetters = mapper.writeValueAsString(requiredLettersMap);
        } catch (JsonProcessingException e) {
            this.requiredLetters = "{}";
        }
    }
    
    /**
     * Checks if the user has enough letters to claim this word bonus
     */
    public boolean canClaimWith(Map<String, Integer> userLetters) {
        if (userLetters == null) {
            return false;
        }
        
        Map<String, Integer> required = getRequiredLettersMap();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            String letter = entry.getKey();
            Integer requiredCount = entry.getValue();
            Integer userCount = userLetters.getOrDefault(letter, 0);
            
            if (userCount < requiredCount) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets the total number of letters required for this word
     */
    public int getTotalLettersRequired() {
        return getRequiredLettersMap().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
    
    /**
     * Gets the unique letters required for this word
     */
    public int getUniqueLettersRequired() {
        return getRequiredLettersMap().size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LetterWord that = (LetterWord) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "LetterWord{" +
                "id=" + id +
                ", word='" + word + '\'' +
                ", requiredLetters='" + requiredLetters + '\'' +
                ", rewardAmount=" + rewardAmount +
                ", active=" + active +
                '}';
    }
}