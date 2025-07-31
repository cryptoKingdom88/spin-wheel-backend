package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_login_missions")
public class DailyLoginMission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    @NotBlank(message = "Mission name cannot be blank")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "spins_granted", nullable = false)
    @Min(value = 1, message = "Spins granted must be at least 1")
    private Integer spinsGranted;
    
    @Column(name = "active", nullable = false)
    @NotNull(message = "Active status cannot be null")
    private Boolean active = true;
    
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
    public DailyLoginMission() {}
    
    // Constructor with required fields
    public DailyLoginMission(String name, String description, Integer spinsGranted) {
        this.name = name;
        this.description = description;
        this.spinsGranted = spinsGranted;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getSpinsGranted() {
        return spinsGranted;
    }
    
    public void setSpinsGranted(Integer spinsGranted) {
        this.spinsGranted = spinsGranted;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyLoginMission that = (DailyLoginMission) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "DailyLoginMission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", spinsGranted=" + spinsGranted +
                ", active=" + active +
                '}';
    }
}