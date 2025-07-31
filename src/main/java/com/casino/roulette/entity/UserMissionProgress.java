package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_mission_progress", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "mission_id"}))
public class UserMissionProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    
    @Column(name = "mission_id", nullable = false)
    @NotNull(message = "Mission ID cannot be null")
    private Long missionId;
    
    @Column(name = "claims_used", nullable = false)
    @Min(value = 0, message = "Claims used cannot be negative")
    private Integer claimsUsed = 0;
    
    @Column(name = "available_claims", nullable = false)
    @Min(value = 0, message = "Available claims cannot be negative")
    private Integer availableClaims = 0;
    
    @Column(name = "last_claim_date")
    private LocalDateTime lastClaimDate;
    
    // Foreign key relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", insertable = false, updatable = false)
    private DepositMission mission;
    
    // Default constructor
    public UserMissionProgress() {}
    
    // Constructor with required fields
    public UserMissionProgress(Long userId, Long missionId) {
        this.userId = userId;
        this.missionId = missionId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getMissionId() {
        return missionId;
    }
    
    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }
    
    public Integer getClaimsUsed() {
        return claimsUsed;
    }
    
    public void setClaimsUsed(Integer claimsUsed) {
        this.claimsUsed = claimsUsed;
    }
    
    public Integer getAvailableClaims() {
        return availableClaims;
    }
    
    public void setAvailableClaims(Integer availableClaims) {
        this.availableClaims = availableClaims;
    }
    
    public LocalDateTime getLastClaimDate() {
        return lastClaimDate;
    }
    
    public void setLastClaimDate(LocalDateTime lastClaimDate) {
        this.lastClaimDate = lastClaimDate;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public DepositMission getMission() {
        return mission;
    }
    
    public void setMission(DepositMission mission) {
        this.mission = mission;
    }
    
    /**
     * Adds available claims when user makes qualifying deposits
     */
    public void addAvailableClaims(Integer claims) {
        if (claims != null && claims > 0) {
            this.availableClaims += claims;
        }
    }
    
    /**
     * Claims all available rewards and updates the last claim date
     * Returns the number of claims processed
     */
    public Integer claimAllAvailable() {
        Integer claimedCount = this.availableClaims;
        this.claimsUsed += this.availableClaims;
        this.availableClaims = 0;
        this.lastClaimDate = LocalDateTime.now();
        return claimedCount;
    }
    
    /**
     * Checks if the user can still claim rewards from this mission
     */
    public boolean canClaim(Integer maxClaims) {
        return this.availableClaims > 0 && 
               (maxClaims == null || this.claimsUsed < maxClaims);
    }
    
    /**
     * Gets the remaining claims available for this mission
     */
    public Integer getRemainingClaims(Integer maxClaims) {
        if (maxClaims == null) {
            return this.availableClaims;
        }
        return Math.min(this.availableClaims, Math.max(0, maxClaims - this.claimsUsed));
    }
    
    /**
     * Checks if user can accumulate more claims (hasn't reached total max)
     */
    public boolean canAccumulateMore(Integer maxClaims) {
        return maxClaims == null || (this.claimsUsed + this.availableClaims) < maxClaims;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserMissionProgress that = (UserMissionProgress) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "UserMissionProgress{" +
                "id=" + id +
                ", userId=" + userId +
                ", missionId=" + missionId +
                ", claimsUsed=" + claimsUsed +
                ", availableClaims=" + availableClaims +
                ", lastClaimDate=" + lastClaimDate +
                '}';
    }
}