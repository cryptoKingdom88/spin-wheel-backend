package com.casino.roulette.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs")
public class TransactionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID cannot be null")
    private Long userId;
    
    @Column(name = "transaction_type", nullable = false, length = 50)
    @NotBlank(message = "Transaction type cannot be blank")
    private String transactionType;
    
    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Foreign key relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Transaction type constants
    public static final String TYPE_DEPOSIT = "DEPOSIT";
    public static final String TYPE_ROULETTE_WIN = "ROULETTE_WIN";
    public static final String TYPE_LETTER_BONUS = "LETTER_BONUS";
    public static final String TYPE_DAILY_LOGIN_SPIN = "DAILY_LOGIN_SPIN";
    public static final String TYPE_FIRST_DEPOSIT_SPIN = "FIRST_DEPOSIT_SPIN";
    public static final String TYPE_DEPOSIT_MISSION_SPIN = "DEPOSIT_MISSION_SPIN";
    public static final String TYPE_SPIN_CONSUMED = "SPIN_CONSUMED";
    public static final String TYPE_LETTER_COLLECTED = "LETTER_COLLECTED";
    public static final String TYPE_ROULETTE_SPIN = "ROULETTE_SPIN";
    
    // Default constructor
    public TransactionLog() {}
    
    // Constructor with required fields
    public TransactionLog(Long userId, String transactionType, String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.description = description;
    }
    
    // Constructor with amount
    public TransactionLog(Long userId, String transactionType, BigDecimal amount, String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
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
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    /**
     * Checks if this transaction involves a cash amount
     */
    public boolean hasCashAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) != 0;
    }
    
    /**
     * Checks if this is a positive cash transaction (credit)
     */
    public boolean isCashCredit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if this is a negative cash transaction (debit)
     */
    public boolean isCashDebit() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Checks if this transaction is of a specific type
     */
    public boolean isOfType(String type) {
        return transactionType != null && transactionType.equals(type);
    }
    
    /**
     * Creates a deposit transaction log
     */
    public static TransactionLog createDepositLog(Long userId, BigDecimal amount) {
        return new TransactionLog(userId, TYPE_DEPOSIT, amount, 
            "User deposit of $" + amount);
    }
    
    /**
     * Creates a roulette win transaction log
     */
    public static TransactionLog createRouletteWinLog(Long userId, BigDecimal amount) {
        return new TransactionLog(userId, TYPE_ROULETTE_WIN, amount, 
            "Roulette spin win of $" + amount);
    }
    
    /**
     * Creates a letter bonus transaction log
     */
    public static TransactionLog createLetterBonusLog(Long userId, BigDecimal amount, String word) {
        return new TransactionLog(userId, TYPE_LETTER_BONUS, amount, 
            "Letter collection bonus for word '" + word + "': $" + amount);
    }
    
    /**
     * Creates a daily login spin transaction log
     */
    public static TransactionLog createDailyLoginSpinLog(Long userId) {
        return new TransactionLog(userId, TYPE_DAILY_LOGIN_SPIN, 
            "Daily login spin granted");
    }
    
    /**
     * Creates a first deposit spin transaction log
     */
    public static TransactionLog createFirstDepositSpinLog(Long userId) {
        return new TransactionLog(userId, TYPE_FIRST_DEPOSIT_SPIN, 
            "First deposit bonus spin granted");
    }
    
    /**
     * Creates a deposit mission spin transaction log
     */
    public static TransactionLog createDepositMissionSpinLog(Long userId, String missionName, Integer spins) {
        return new TransactionLog(userId, TYPE_DEPOSIT_MISSION_SPIN, 
            spins + " spin(s) granted from mission: " + missionName);
    }
    
    /**
     * Creates a spin consumed transaction log
     */
    public static TransactionLog createSpinConsumedLog(Long userId) {
        return new TransactionLog(userId, TYPE_SPIN_CONSUMED, 
            "Free spin consumed for roulette");
    }
    
    /**
     * Creates a letter collected transaction log
     */
    public static TransactionLog createLetterCollectedLog(Long userId, String letter) {
        return new TransactionLog(userId, TYPE_LETTER_COLLECTED, 
            "Letter '" + letter + "' collected from roulette spin");
    }
    
    /**
     * Creates a roulette spin transaction log
     */
    public static TransactionLog createRouletteSpinLog(Long userId, String slotType, String slotValue) {
        String description = "Roulette spin result: " + slotType + " - " + slotValue;
        return new TransactionLog(userId, "ROULETTE_SPIN", description);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionLog that = (TransactionLog) o;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "TransactionLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}