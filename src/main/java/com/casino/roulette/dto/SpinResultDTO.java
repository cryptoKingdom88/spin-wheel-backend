package com.casino.roulette.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * DTO for roulette spin results
 */
public class SpinResultDTO {
    
    @NotNull
    @Pattern(regexp = "CASH|LETTER", message = "Type must be either CASH or LETTER")
    private String type;
    
    @NotNull
    private String value;
    
    @PositiveOrZero
    private BigDecimal cash;
    
    @Pattern(regexp = "[A-Z]", message = "Letter must be a single uppercase letter")
    private String letter;
    
    @PositiveOrZero
    private Integer remainingSpins;
    
    // Default constructor
    public SpinResultDTO() {}
    
    // Constructor for cash wins
    public SpinResultDTO(String type, String value, BigDecimal cash, Integer remainingSpins) {
        this.type = type;
        this.value = value;
        this.cash = cash;
        this.remainingSpins = remainingSpins;
    }
    
    // Constructor for letter wins
    public SpinResultDTO(String type, String value, String letter, Integer remainingSpins) {
        this.type = type;
        this.value = value;
        this.letter = letter;
        this.remainingSpins = remainingSpins;
    }
    
    // Full constructor
    public SpinResultDTO(String type, String value, BigDecimal cash, String letter, Integer remainingSpins) {
        this.type = type;
        this.value = value;
        this.cash = cash;
        this.letter = letter;
        this.remainingSpins = remainingSpins;
    }
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public BigDecimal getCash() {
        return cash;
    }
    
    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }
    
    public String getLetter() {
        return letter;
    }
    
    public void setLetter(String letter) {
        this.letter = letter;
    }
    
    public Integer getRemainingSpins() {
        return remainingSpins;
    }
    
    public void setRemainingSpins(Integer remainingSpins) {
        this.remainingSpins = remainingSpins;
    }
    
    @Override
    public String toString() {
        return "SpinResultDTO{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                ", cash=" + cash +
                ", letter='" + letter + '\'' +
                ", remainingSpins=" + remainingSpins +
                '}';
    }
}