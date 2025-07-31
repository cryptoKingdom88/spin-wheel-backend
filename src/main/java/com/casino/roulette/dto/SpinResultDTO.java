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
    private BigDecimal cashWon;
    
    @Pattern(regexp = "[A-Z]", message = "Letter must be a single uppercase letter")
    private String letterWon;
    
    @PositiveOrZero
    private Integer remainingSpins;
    
    // Default constructor
    public SpinResultDTO() {}
    
    // Constructor for cash wins
    public SpinResultDTO(String type, String value, BigDecimal cashWon, Integer remainingSpins) {
        this.type = type;
        this.value = value;
        this.cashWon = cashWon;
        this.remainingSpins = remainingSpins;
    }
    
    // Constructor for letter wins
    public SpinResultDTO(String type, String value, String letterWon, Integer remainingSpins) {
        this.type = type;
        this.value = value;
        this.letterWon = letterWon;
        this.remainingSpins = remainingSpins;
    }
    
    // Full constructor
    public SpinResultDTO(String type, String value, BigDecimal cashWon, String letterWon, Integer remainingSpins) {
        this.type = type;
        this.value = value;
        this.cashWon = cashWon;
        this.letterWon = letterWon;
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
    
    public BigDecimal getCashWon() {
        return cashWon;
    }
    
    public void setCashWon(BigDecimal cashWon) {
        this.cashWon = cashWon;
    }
    
    public String getLetterWon() {
        return letterWon;
    }
    
    public void setLetterWon(String letterWon) {
        this.letterWon = letterWon;
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
                ", cashWon=" + cashWon +
                ", letterWon='" + letterWon + '\'' +
                ", remainingSpins=" + remainingSpins +
                '}';
    }
}