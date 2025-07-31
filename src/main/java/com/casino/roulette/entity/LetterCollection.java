package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "letter_collections", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "letter"}))
public class LetterCollection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    
    @Column(name = "letter", nullable = false, length = 1)
    @NotBlank(message = "Letter cannot be blank")
    @Size(min = 1, max = 1, message = "Letter must be exactly one character")
    private String letter;
    
    @Column(name = "count", nullable = false)
    @Min(value = 0, message = "Count cannot be negative")
    private Integer count = 0;
    
    // Foreign key relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    // Default constructor
    public LetterCollection() {}
    
    // Constructor with required fields
    public LetterCollection(Long userId, String letter) {
        this.userId = userId;
        this.letter = letter != null ? letter.toUpperCase() : null;
    }
    
    // Constructor with count
    public LetterCollection(Long userId, String letter, Integer count) {
        this.userId = userId;
        this.letter = letter != null ? letter.toUpperCase() : null;
        this.count = count;
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
    
    public String getLetter() {
        return letter;
    }
    
    public void setLetter(String letter) {
        this.letter = letter != null ? letter.toUpperCase() : null;
    }
    
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    /**
     * Increments the letter count by 1
     */
    public void incrementCount() {
        this.count++;
    }
    
    /**
     * Increments the letter count by the specified amount
     */
    public void incrementCount(Integer amount) {
        if (amount != null && amount > 0) {
            this.count += amount;
        }
    }
    
    /**
     * Decrements the letter count by the specified amount
     * Ensures count doesn't go below 0
     */
    public void decrementCount(Integer amount) {
        if (amount != null && amount > 0) {
            this.count = Math.max(0, this.count - amount);
        }
    }
    
    /**
     * Checks if the user has at least the specified amount of this letter
     */
    public boolean hasAtLeast(Integer requiredAmount) {
        return requiredAmount != null && this.count >= requiredAmount;
    }
    
    /**
     * Checks if the collection is empty (count is 0)
     */
    public boolean isEmpty() {
        return this.count == 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LetterCollection that = (LetterCollection) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "LetterCollection{" +
                "id=" + id +
                ", userId=" + userId +
                ", letter='" + letter + '\'' +
                ", count=" + count +
                '}';
    }
}