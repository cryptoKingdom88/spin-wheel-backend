package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_missions")
public class DepositMission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    @NotBlank(message = "Mission name cannot be blank")
    private String name;
    
    @Column(name = "min_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Minimum amount cannot be null")
    @DecimalMin(value = "0.01", message = "Minimum amount must be greater than 0")
    private BigDecimal minAmount;
    
    @Column(name = "max_amount", precision = 10, scale = 2)
    private BigDecimal maxAmount;
    
    @Column(name = "spins_granted", nullable = false)
    @NotNull(message = "Spins granted cannot be null")
    @Min(value = 1, message = "Spins granted must be at least 1")
    private Integer spinsGranted;
    
    @Column(name = "max_claims", nullable = false)
    @NotNull(message = "Max claims cannot be null")
    @Min(value = 1, message = "Max claims must be at least 1")
    private Integer maxClaims;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Default constructor
    public DepositMission() {}
    
    // Constructor with required fields
    public DepositMission(String name, BigDecimal minAmount, Integer spinsGranted, Integer maxClaims) {
        this.name = name;
        this.minAmount = minAmount;
        this.spinsGranted = spinsGranted;
        this.maxClaims = maxClaims;
    }
    
    // Getters and Setters
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
    
    public BigDecimal getMinAmount() {
        return minAmount;
    }
    
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }
    
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }
    
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
    
    public Integer getSpinsGranted() {
        return spinsGranted;
    }
    
    public void setSpinsGranted(Integer spinsGranted) {
        this.spinsGranted = spinsGranted;
    }
    
    public Integer getMaxClaims() {
        return maxClaims;
    }
    
    public void setMaxClaims(Integer maxClaims) {
        this.maxClaims = maxClaims;
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
     * Checks if a deposit amount falls within this mission's range
     */
    public boolean isAmountInRange(BigDecimal amount) {
        if (amount == null || amount.compareTo(minAmount) < 0) {
            return false;
        }
        return maxAmount == null || amount.compareTo(maxAmount) <= 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositMission that = (DepositMission) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "DepositMission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                ", spinsGranted=" + spinsGranted +
                ", maxClaims=" + maxClaims +
                ", active=" + active +
                '}';
    }
}