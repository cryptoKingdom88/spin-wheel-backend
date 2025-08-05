package com.casino.roulette.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO for mission information
 */
public class MissionDTO {
    
    @NotNull
    private Long id;
    
    @NotBlank
    private String name;
    
    private String description;
    
    @PositiveOrZero
    private Integer spinsPerClaim; // Spins granted per single claim (e.g., 2 for $500 deposit)
    
    @PositiveOrZero
    private Integer pendingSpins; // Total spins waiting to be claimed
    
    @NotNull
    private Boolean canClaim;
    
    @PositiveOrZero
    private Integer claimsUsed;
    
    @PositiveOrZero
    private Integer maxClaims;
    
    // Default constructor
    public MissionDTO() {}
    
    // Full constructor
    public MissionDTO(Long id, String name, String description, Integer spinsPerClaim, 
                     Integer pendingSpins, Boolean canClaim, Integer claimsUsed, Integer maxClaims) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.spinsPerClaim = spinsPerClaim;
        this.pendingSpins = pendingSpins;
        this.canClaim = canClaim;
        this.claimsUsed = claimsUsed;
        this.maxClaims = maxClaims;
    }
    
    // Constructor without description
    public MissionDTO(Long id, String name, Integer spinsPerClaim, Integer pendingSpins, 
                     Boolean canClaim, Integer claimsUsed, Integer maxClaims) {
        this.id = id;
        this.name = name;
        this.spinsPerClaim = spinsPerClaim;
        this.pendingSpins = pendingSpins;
        this.canClaim = canClaim;
        this.claimsUsed = claimsUsed;
        this.maxClaims = maxClaims;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getSpinsPerClaim() {
        return spinsPerClaim;
    }
    
    public void setSpinsPerClaim(Integer spinsPerClaim) {
        this.spinsPerClaim = spinsPerClaim;
    }
    
    public Integer getPendingSpins() {
        return pendingSpins;
    }
    
    public void setPendingSpins(Integer pendingSpins) {
        this.pendingSpins = pendingSpins;
    }
    

    
    public Boolean getCanClaim() {
        return canClaim;
    }
    
    public void setCanClaim(Boolean canClaim) {
        this.canClaim = canClaim;
    }
    
    public Integer getClaimsUsed() {
        return claimsUsed;
    }
    
    public void setClaimsUsed(Integer claimsUsed) {
        this.claimsUsed = claimsUsed;
    }
    
    public Integer getMaxClaims() {
        return maxClaims;
    }
    
    public void setMaxClaims(Integer maxClaims) {
        this.maxClaims = maxClaims;
    }
    
    /**
     * Check if mission has remaining claims available
     */
    public boolean hasRemainingClaims() {
        if (maxClaims == null || claimsUsed == null) {
            return false;
        }
        return claimsUsed < maxClaims;
    }
    
    /**
     * Get remaining claims count
     */
    public Integer getRemainingClaims() {
        if (maxClaims == null || claimsUsed == null) {
            return 0;
        }
        return Math.max(0, maxClaims - claimsUsed);
    }
    
    @Override
    public String toString() {
        return "MissionDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", spinsPerClaim=" + spinsPerClaim +
                ", pendingSpins=" + pendingSpins +
                ", canClaim=" + canClaim +
                ", claimsUsed=" + claimsUsed +
                ", maxClaims=" + maxClaims +
                '}';
    }
}