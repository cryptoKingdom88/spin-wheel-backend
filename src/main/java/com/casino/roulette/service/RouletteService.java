package com.casino.roulette.service;

import com.casino.roulette.dto.SpinResultDTO;
import com.casino.roulette.entity.RouletteSlot;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.RouletteSlotRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@Transactional
public class RouletteService {
    
    private final RouletteSlotRepository rouletteSlotRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    private final LetterService letterService;
    private final SecureRandom random;
    
    @Autowired
    public RouletteService(RouletteSlotRepository rouletteSlotRepository,
                          TransactionLogRepository transactionLogRepository,
                          UserService userService,
                          LetterService letterService) {
        this.rouletteSlotRepository = rouletteSlotRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.userService = userService;
        this.letterService = letterService;
        this.random = new SecureRandom();
    }
    
    /**
     * Spin the roulette wheel for a user
     */
    public SpinResultDTO spinRoulette(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Check if user has sufficient spins
        if (!userService.hasSufficientSpins(userId, 1)) {
            throw new IllegalStateException("User has no available spins");
        }
        
        // Get active slots for weighted selection
        List<RouletteSlot> activeSlots = rouletteSlotRepository.findByActiveTrue();
        if (activeSlots.isEmpty()) {
            throw new IllegalStateException("No active roulette slots available");
        }
        
        // Consume one spin
        boolean spinConsumed = userService.consumeSpins(userId, 1);
        if (!spinConsumed) {
            throw new IllegalStateException("Failed to consume spin");
        }
        
        // Perform weighted random selection
        RouletteSlot selectedSlot = performWeightedSelection(activeSlots);
        
        // Get user's remaining spins after consumption
        User user = userService.getUser(userId);
        Integer remainingSpins = user != null ? user.getAvailableSpins() : 0;
        
        // Process the result based on slot type
        SpinResultDTO result;
        if (selectedSlot.isCashSlot()) {
            result = processCashWin(userId, selectedSlot, remainingSpins);
        } else if (selectedSlot.isLetterSlot()) {
            result = processLetterWin(userId, selectedSlot, remainingSpins);
        } else {
            throw new IllegalStateException("Unknown slot type: " + selectedSlot.getSlotType());
        }
        
        // Log the spin transaction
        TransactionLog spinLog = TransactionLog.createRouletteSpinLog(
            userId, selectedSlot.getSlotType().toString(), selectedSlot.getSlotValue());
        transactionLogRepository.save(spinLog);
        
        return result;
    }
    
    /**
     * Get current roulette configuration
     */
    @Transactional(readOnly = true)
    public List<RouletteSlot> getRouletteConfiguration() {
        return rouletteSlotRepository.findActiveSlotsByWeightDesc();
    }
    
    /**
     * Update roulette slots configuration (admin function)
     */
    public void updateRouletteSlots(List<RouletteSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Slots list cannot be null or empty");
        }
        
        // Validate slots
        for (RouletteSlot slot : slots) {
            if (slot.getSlotType() == null) {
                throw new IllegalArgumentException("Slot type cannot be null");
            }
            if (slot.getSlotValue() == null || slot.getSlotValue().trim().isEmpty()) {
                throw new IllegalArgumentException("Slot value cannot be null or empty");
            }
            if (slot.getWeight() == null || slot.getWeight() < 1) {
                throw new IllegalArgumentException("Slot weight must be at least 1");
            }
        }
        
        // Save all slots
        rouletteSlotRepository.saveAll(slots);
    }
    
    /**
     * Check if roulette is available (has active slots)
     */
    @Transactional(readOnly = true)
    public boolean isRouletteAvailable() {
        return rouletteSlotRepository.existsByActiveTrue();
    }
    
    /**
     * Get total weight of all active slots
     */
    @Transactional(readOnly = true)
    public Long getTotalActiveWeight() {
        return rouletteSlotRepository.getTotalWeightForActiveSlots();
    }
    
    /**
     * Get count of active slots by type
     */
    @Transactional(readOnly = true)
    public long getActiveSlotCountByType(RouletteSlot.SlotType slotType) {
        if (slotType == null) {
            throw new IllegalArgumentException("Slot type cannot be null");
        }
        return rouletteSlotRepository.countByActiveTrueAndSlotType(slotType);
    }
    
    /**
     * Perform weighted random selection from active slots
     */
    private RouletteSlot performWeightedSelection(List<RouletteSlot> slots) {
        // Calculate total weight
        int totalWeight = slots.stream()
            .mapToInt(RouletteSlot::getWeight)
            .sum();
        
        if (totalWeight <= 0) {
            throw new IllegalStateException("Total weight must be positive");
        }
        
        // Generate random number between 1 and totalWeight (inclusive)
        int randomValue = random.nextInt(totalWeight) + 1;
        
        // Find the selected slot using cumulative weights
        int cumulativeWeight = 0;
        for (RouletteSlot slot : slots) {
            cumulativeWeight += slot.getWeight();
            if (randomValue <= cumulativeWeight) {
                return slot;
            }
        }
        
        // Fallback (should never reach here with valid weights)
        return slots.get(slots.size() - 1);
    }
    
    /**
     * Process cash win result
     */
    private SpinResultDTO processCashWin(Long userId, RouletteSlot slot, Integer remainingSpins) {
        try {
            BigDecimal cashAmount = new BigDecimal(slot.getSlotValue());
            
            // Update user's cash balance
            userService.updateCashBalance(userId, cashAmount);
            
            // Create result DTO
            return new SpinResultDTO(
                slot.getSlotType().toString(),
                slot.getSlotValue(),
                cashAmount,
                remainingSpins
            );
            
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid cash value in slot: " + slot.getSlotValue(), e);
        }
    }
    
    /**
     * Process letter win result
     */
    private SpinResultDTO processLetterWin(Long userId, RouletteSlot slot, Integer remainingSpins) {
        String letter = slot.getSlotValue().toUpperCase();
        
        // Validate letter format
        if (letter.length() != 1 || !Character.isLetter(letter.charAt(0))) {
            throw new IllegalStateException("Invalid letter value in slot: " + slot.getSlotValue());
        }
        
        // Add letter to user's collection
        letterService.addLetterToCollection(userId, letter);
        
        // Create result DTO
        return new SpinResultDTO(
            slot.getSlotType().toString(),
            slot.getSlotValue(),
            letter,
            remainingSpins
        );
    }
}