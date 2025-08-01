package com.casino.roulette.service;

import com.casino.roulette.dto.SpinResultDTO;
import com.casino.roulette.entity.RouletteSlot;
import com.casino.roulette.entity.User;
import com.casino.roulette.repository.RouletteSlotRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import com.casino.roulette.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SpinRemainingCountTest {
    
    @Autowired
    private RouletteService rouletteService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RouletteSlotRepository rouletteSlotRepository;
    
    @Autowired
    private TransactionLogRepository transactionLogRepository;
    
    private User testUser;
    private final Long TEST_USER_ID = 999L;
    
    @BeforeEach
    void setUp() {
        // Clean up
        transactionLogRepository.deleteAll();
        rouletteSlotRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test user with 7 spins
        testUser = new User(TEST_USER_ID);
        testUser.setAvailableSpins(7);
        testUser.setCashBalance(BigDecimal.ZERO);
        userRepository.save(testUser);
        
        // Create test roulette slots
        RouletteSlot cashSlot = new RouletteSlot(RouletteSlot.SlotType.CASH, "1.00", 50, true);
        RouletteSlot letterSlot = new RouletteSlot(RouletteSlot.SlotType.LETTER, "A", 50, true);
        rouletteSlotRepository.saveAll(Arrays.asList(cashSlot, letterSlot));
    }
    
    @Test
    void testSpinRemainingCountDecreasesCorrectly() {
        // Given: User has 7 spins
        User userBefore = userRepository.findById(TEST_USER_ID).orElse(null);
        assertNotNull(userBefore);
        assertEquals(7, userBefore.getAvailableSpins());
        
        // When: User spins the roulette
        SpinResultDTO result = rouletteService.spinRoulette(TEST_USER_ID);
        
        // Then: remainingSpins in result should be 6 (7 - 1)
        assertEquals(Integer.valueOf(6), result.getRemainingSpins());
        
        // And: User's actual spin count should also be 6
        User userAfter = userRepository.findById(TEST_USER_ID).orElse(null);
        assertNotNull(userAfter);
        assertEquals(6, userAfter.getAvailableSpins());
        
        // Verify the result is valid
        assertNotNull(result.getType());
        assertNotNull(result.getValue());
        assertTrue(result.getType().equals("CASH") || result.getType().equals("LETTER"));
    }
    
    @Test
    void testMultipleSpinsDecrementCorrectly() {
        // Given: User has 7 spins
        assertEquals(7, userRepository.findById(TEST_USER_ID).orElse(new User()).getAvailableSpins());
        
        // When: User spins 3 times
        SpinResultDTO result1 = rouletteService.spinRoulette(TEST_USER_ID);
        SpinResultDTO result2 = rouletteService.spinRoulette(TEST_USER_ID);
        SpinResultDTO result3 = rouletteService.spinRoulette(TEST_USER_ID);
        
        // Then: Each result should show correct remaining spins
        assertEquals(Integer.valueOf(6), result1.getRemainingSpins()); // 7 - 1 = 6
        assertEquals(Integer.valueOf(5), result2.getRemainingSpins()); // 6 - 1 = 5
        assertEquals(Integer.valueOf(4), result3.getRemainingSpins()); // 5 - 1 = 4
        
        // And: Final user spin count should be 4
        User finalUser = userRepository.findById(TEST_USER_ID).orElse(null);
        assertNotNull(finalUser);
        assertEquals(4, finalUser.getAvailableSpins());
    }
}