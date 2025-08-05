package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @NotNull
    private Long id; // External user ID from main system
    
    @Column(name = "cash_balance", precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Cash balance cannot be negative")
    private BigDecimal cashBalance = BigDecimal.ZERO;
    
    @Column(name = "available_spins")
    @Min(value = 0, message = "Available spins cannot be negative")
    private Integer availableSpins = 0;
    
    @Column(name = "first_deposit_bonus_used")
    private Boolean firstDepositBonusUsed = false;
    
    @Column(name = "last_daily_login")
    private LocalDateTime lastDailyLogin;
    
    @Column(name = "last_daily_mission_claim")
    private LocalDateTime lastDailyMissionClaim;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Default constructor
    public User() {}
    
    // Constructor with ID
    public User(Long id) {
        this.id = id;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public BigDecimal getCashBalance() {
        return cashBalance;
    }
    
    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
    
    public Integer getAvailableSpins() {
        return availableSpins;
    }
    
    public void setAvailableSpins(Integer availableSpins) {
        this.availableSpins = availableSpins;
    }
    
    public Boolean getFirstDepositBonusUsed() {
        return firstDepositBonusUsed;
    }
    
    public void setFirstDepositBonusUsed(Boolean firstDepositBonusUsed) {
        this.firstDepositBonusUsed = firstDepositBonusUsed;
    }
    
    public LocalDateTime getLastDailyLogin() {
        return lastDailyLogin;
    }
    
    public void setLastDailyLogin(LocalDateTime lastDailyLogin) {
        this.lastDailyLogin = lastDailyLogin;
    }
    
    public LocalDateTime getLastDailyMissionClaim() {
        return lastDailyMissionClaim;
    }
    
    public void setLastDailyMissionClaim(LocalDateTime lastDailyMissionClaim) {
        this.lastDailyMissionClaim = lastDailyMissionClaim;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", cashBalance=" + cashBalance +
                ", availableSpins=" + availableSpins +
                ", firstDepositBonusUsed=" + firstDepositBonusUsed +
                ", lastDailyLogin=" + lastDailyLogin +
                ", lastDailyMissionClaim=" + lastDailyMissionClaim +
                '}';
    }
}