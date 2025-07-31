package com.casino.roulette.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for user letter collection
 */
public class LetterCollectionDTO {
    
    @NotNull
    @Pattern(regexp = "[A-Z]", message = "Letter must be a single uppercase letter")
    private String letter;
    
    @NotNull
    @PositiveOrZero
    private Integer count;
    
    // Default constructor
    public LetterCollectionDTO() {}
    
    // Constructor
    public LetterCollectionDTO(String letter, Integer count) {
        this.letter = letter;
        this.count = count;
    }
    
    // Getters and setters
    public String getLetter() {
        return letter;
    }
    
    public void setLetter(String letter) {
        this.letter = letter;
    }
    
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }
    
    /**
     * Check if user has any of this letter
     */
    public boolean hasLetter() {
        return count != null && count > 0;
    }
    
    /**
     * Check if user has at least the specified amount of this letter
     */
    public boolean hasAtLeast(int requiredCount) {
        return count != null && count >= requiredCount;
    }
    
    @Override
    public String toString() {
        return "LetterCollectionDTO{" +
                "letter='" + letter + '\'' +
                ", count=" + count +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LetterCollectionDTO that = (LetterCollectionDTO) o;
        
        if (!letter.equals(that.letter)) return false;
        return count.equals(that.count);
    }
    
    @Override
    public int hashCode() {
        int result = letter.hashCode();
        result = 31 * result + count.hashCode();
        return result;
    }
}