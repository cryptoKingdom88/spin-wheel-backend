package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "roulette_slots")
public class RouletteSlot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "slot_type", nullable = false, length = 10)
    @NotNull(message = "Slot type cannot be null")
    @Enumerated(EnumType.STRING)
    private SlotType slotType;
    
    @Column(name = "slot_value", nullable = false, length = 50)
    @NotBlank(message = "Slot value cannot be blank")
    private String slotValue;
    
    @Column(name = "weight", nullable = false)
    @NotNull(message = "Weight cannot be null")
    @Min(value = 1, message = "Weight must be at least 1")
    private Integer weight = 1;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Enum for slot types
    public enum SlotType {
        CASH, LETTER
    }
    
    // Default constructor
    public RouletteSlot() {}
    
    // Constructor with required fields
    public RouletteSlot(SlotType slotType, String slotValue, Integer weight) {
        this.slotType = slotType;
        this.slotValue = slotValue;
        this.weight = weight;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public SlotType getSlotType() {
        return slotType;
    }
    
    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }
    
    public String getSlotValue() {
        return slotValue;
    }
    
    public void setSlotValue(String slotValue) {
        this.slotValue = slotValue;
    }
    
    public Integer getWeight() {
        return weight;
    }
    
    public void setWeight(Integer weight) {
        this.weight = weight;
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
     * Checks if this slot is a cash reward slot
     */
    public boolean isCashSlot() {
        return SlotType.CASH.equals(this.slotType);
    }
    
    /**
     * Checks if this slot is a letter reward slot
     */
    public boolean isLetterSlot() {
        return SlotType.LETTER.equals(this.slotType);
    }
    
    /**
     * Gets the cash value if this is a cash slot, null otherwise
     */
    public String getCashValue() {
        return isCashSlot() ? this.slotValue : null;
    }
    
    /**
     * Gets the letter value if this is a letter slot, null otherwise
     */
    public String getLetterValue() {
        return isLetterSlot() ? this.slotValue : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouletteSlot that = (RouletteSlot) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "RouletteSlot{" +
                "id=" + id +
                ", slotType=" + slotType +
                ", slotValue='" + slotValue + '\'' +
                ", weight=" + weight +
                ", active=" + active +
                '}';
    }
}