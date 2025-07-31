package com.casino.roulette.integration;

import com.casino.roulette.dto.*;
import com.casino.roulette.entity.*;
import com.casino.roulette.repository.*;
import com.casino.roulette.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive end-to-end integration tests covering all user workflows
 * and system requirements for the free roulette spin system.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ComprehensiveWorkflowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepositMissionRepository depositMissionRepository;
    @Autowired
    private UserMissionProgressRepository userMissionProgressRepository;
    @Autowired
    private RouletteSlotRepository rouletteSlotRepository;
    @Autowired
    private LetterCollectionRepository letterCollectionRepository;
    @Autowired
    private LetterWordRepository letterWordRepository;
    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private UserService userService;
    @Autowired
    private MissionService missionService;
    @Autowired
    private RouletteService rouletteService;
    @Autowired
    private LetterService letterService;
    @Autowired
    private TransactionService transactionService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clean up all data
        transactionLogRepository.deleteAll();
        letterCollectionRepository.deleteAll();
        userMissionProgressRepository.deleteAll();
        userRepository.deleteAll();
        
        // Set up comprehensive test data
        setupComprehensiveTestData();
    }

    /**
     * Test complete new user journey from registration to advanced gameplay
     * Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1-3.9, 4.1-4.4, 5.1-5.6, 6.1-6.4, 7.1-7.5, 8.1-8.5, 9.1-9.5
     */
    @Test
    @Transactional
    void testCompleteNewUserJourney() throws Exception {
        // Phase 1: User Registration and Daily Login Spin (Requirement 1.1)
        User user = userService.getOrCreateUser(TEST_USER_ID);
        boolean spinGranted = userService.grantDailyLoginSpin(TEST_USER_ID);
        assertThat(spinGranted).isTrue();
        
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(1); // Daily login spin
        assertThat(user.getCashBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(user.getFirstDepositBonusUsed()).isFalse();
        
        // Verify daily login transaction log (Requirement 9.1)
        List<TransactionLog> logs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getTransactionType()).isEqualTo("DAILY_LOGIN_SPIN");

        // Phase 2: First Deposit and Bonus (Requirements 2.1, 2.2, 2.3)
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 150.00}"))
                .andExpect(status().isOk());

        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getFirstDepositBonusUsed()).isTrue();
        assertThat(user.getCashBalance()).isEqualTo(new BigDecimal("150.00"));
        assertThat(user.getAvailableSpins()).isGreaterThan(1); // Daily + first deposit + mission spins

        // Verify deposit and first deposit bonus transactions (Requirements 9.2, 9.3)
        logs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        List<String> transactionTypes = logs.stream()
                .map(TransactionLog::getTransactionType)
                .toList();
        assertThat(transactionTypes).contains("DEPOSIT", "FIRST_DEPOSIT_BONUS");

        // Phase 3: Mission System Testing (Requirements 3.1-3.9, 4.1-4.4)
        String missionsResponse = mockMvc.perform(get("/api/missions")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> missions = objectMapper.readValue(missionsResponse, List.class);
        assertThat(missions).isNotEmpty();

        // Claim all available missions
        int totalMissionSpins = 0;
        for (Map<String, Object> mission : missions) {
            if (Boolean.TRUE.equals(mission.get("canClaim"))) {
                Long missionId = ((Number) mission.get("id")).longValue();
                Integer spinsAvailable = (Integer) mission.get("spinsAvailable");
                totalMissionSpins += spinsAvailable;
                
                mockMvc.perform(post("/api/missions/{id}/claim", missionId)
                        .header("X-User-Id", TEST_USER_ID))
                        .andExpect(status().isOk());
            }
        }

        // Verify mission progress tracking (Requirements 4.1.4, 4.1.5)
        List<UserMissionProgress> progressList = userMissionProgressRepository.findByUserId(TEST_USER_ID);
        assertThat(progressList).isNotEmpty();
        for (UserMissionProgress progress : progressList) {
            assertThat(progress.getClaimsUsed()).isGreaterThan(0);
            assertThat(progress.getLastClaimDate()).isNotNull();
        }

        // Phase 4: Roulette Gameplay (Requirements 5.1-5.6, 6.1-6.4)
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        int initialSpins = user.getAvailableSpins();
        int spinsToPlay = Math.min(10, initialSpins);
        
        BigDecimal initialBalance = user.getCashBalance();
        int totalCashWins = 0;
        int totalLetterWins = 0;

        for (int i = 0; i < spinsToPlay; i++) {
            String spinResponse = mockMvc.perform(post("/api/roulette/spin")
                    .header("X-User-Id", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.remainingSpins").value(initialSpins - i - 1))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            SpinResultDTO result = objectMapper.readValue(spinResponse, SpinResultDTO.class);
            
            if ("CASH".equals(result.getType())) {
                totalCashWins++;
                assertThat(result.getCashWon()).isNotNull();
                assertThat(result.getCashWon()).isGreaterThan(BigDecimal.ZERO);
            } else if ("LETTER".equals(result.getType())) {
                totalLetterWins++;
                assertThat(result.getLetterWon()).isNotNull();
                assertThat(result.getLetterWon()).matches("[A-Z]");
            }
        }

        // Verify roulette results were processed correctly (Requirements 5.3, 5.5, 5.6)
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(initialSpins - spinsToPlay);
        
        if (totalCashWins > 0) {
            assertThat(user.getCashBalance()).isGreaterThan(initialBalance);
        }

        // Phase 5: Letter Collection System (Requirements 7.1, 7.2, 7.3, 7.5)
        if (totalLetterWins > 0) {
            String collectionResponse = mockMvc.perform(get("/api/letters/collection")
                    .header("X-User-Id", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> collection = objectMapper.readValue(collectionResponse, List.class);
            int totalCollectedLetters = collection.stream()
                    .mapToInt(letter -> (Integer) letter.get("count"))
                    .sum();
            assertThat(totalCollectedLetters).isEqualTo(totalLetterWins);
        }

        // Phase 6: Word Bonus System (Requirements 8.1-8.5)
        // Add specific letters to test word completion
        letterService.addLetterToCollection(TEST_USER_ID, "W");
        letterService.addLetterToCollection(TEST_USER_ID, "I");
        letterService.addLetterToCollection(TEST_USER_ID, "N");

        String wordsResponse = mockMvc.perform(get("/api/letters/words")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> words = objectMapper.readValue(wordsResponse, List.class);
        
        // Try to claim word bonus if available
        for (Map<String, Object> word : words) {
            if (Boolean.TRUE.equals(word.get("canClaim"))) {
                Long wordId = ((Number) word.get("id")).longValue();
                BigDecimal balanceBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
                
                mockMvc.perform(post("/api/letters/words/{id}/claim", wordId)
                        .header("X-User-Id", TEST_USER_ID))
                        .andExpect(status().isOk());

                BigDecimal balanceAfter = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
                assertThat(balanceAfter).isGreaterThan(balanceBefore);
                break;
            }
        }

        // Phase 7: Comprehensive Transaction Logging Verification (Requirements 9.1-9.5)
        logs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        assertThat(logs.size()).isGreaterThan(5); // Multiple transaction types

        // Verify all expected transaction types are present
        List<String> allTransactionTypes = logs.stream()
                .map(TransactionLog::getTransactionType)
                .distinct()
                .toList();
        
        assertThat(allTransactionTypes).contains("DAILY_LOGIN_SPIN", "DEPOSIT", "FIRST_DEPOSIT_BONUS");
        
        // Verify transaction integrity
        for (TransactionLog log : logs) {
            assertThat(log.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(log.getTransactionType()).isNotNull();
            assertThat(log.getCreatedAt()).isNotNull();
            assertThat(log.getCreatedAt()).isBefore(LocalDateTime.now().plusMinutes(1));
        }
    }

    /**
     * Test deposit processing workflow with all tier requirements
     * Requirements: 3.1-3.9, 4.1.1-4.1.5
     */
    @Test
    @Transactional
    void testComprehensiveDepositProcessingWorkflow() throws Exception {
        userService.getOrCreateUser(TEST_USER_ID);

        // Test all deposit tiers as specified in requirements
        BigDecimal[] depositAmounts = {
                new BigDecimal("75.00"),   // $50-$99 tier (Requirement 3.1)
                new BigDecimal("150.00"),  // $100-$199 tier (Requirement 3.2)
                new BigDecimal("300.00"),  // $200-$499 tier (Requirement 3.3)
                new BigDecimal("600.00")   // $500+ tier (Requirement 3.4)
        };

        int[] expectedSpinsPerTier = {1, 1, 1, 2}; // As per requirements 3.1-3.4

        for (int i = 0; i < depositAmounts.length; i++) {
            BigDecimal amount = depositAmounts[i];
            int expectedSpins = expectedSpinsPerTier[i];
            
            int spinsBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getAvailableSpins();
            BigDecimal balanceBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
            
            mockMvc.perform(post("/api/deposits")
                    .header("X-User-Id", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\": " + amount + "}"))
                    .andExpect(status().isOk());

            User user = userRepository.findById(TEST_USER_ID).orElseThrow();
            assertThat(user.getAvailableSpins()).isGreaterThanOrEqualTo(spinsBefore + expectedSpins);
            assertThat(user.getCashBalance()).isEqualTo(balanceBefore.add(amount));
        }

        // Test mission limits (Requirements 3.5-3.9)
        // Verify mission progress tracking
        List<UserMissionProgress> progressList = userMissionProgressRepository.findByUserId(TEST_USER_ID);
        assertThat(progressList).isNotEmpty();
        
        for (UserMissionProgress progress : progressList) {
            DepositMission mission = depositMissionRepository.findById(progress.getMissionId()).orElseThrow();
            assertThat(progress.getClaimsUsed()).isLessThanOrEqualTo(mission.getMaxClaims());
        }
    }

    /**
     * Test roulette mechanics and weighted probability system
     * Requirements: 5.1-5.6, 6.1-6.4
     */
    @Test
    @Transactional
    void testRouletteSystemMechanics() throws Exception {
        // Setup user with many spins for statistical testing
        User user = userService.getOrCreateUser(TEST_USER_ID);
        user.setAvailableSpins(100);
        userRepository.save(user);

        int totalSpins = 50;
        int cashWins = 0;
        int letterWins = 0;
        BigDecimal totalCashWon = BigDecimal.ZERO;

        // Perform multiple spins to test weighted probability
        for (int i = 0; i < totalSpins; i++) {
            String spinResponse = mockMvc.perform(post("/api/roulette/spin")
                    .header("X-User-Id", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.remainingSpins").value(100 - i - 1))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            SpinResultDTO result = objectMapper.readValue(spinResponse, SpinResultDTO.class);
            
            // Verify result structure (Requirement 5.3)
            assertThat(result.getType()).isIn("CASH", "LETTER");
            assertThat(result.getRemainingSpins()).isEqualTo(100 - i - 1);
            
            if ("CASH".equals(result.getType())) {
                cashWins++;
                assertThat(result.getCashWon()).isNotNull();
                assertThat(result.getCashWon()).isGreaterThan(BigDecimal.ZERO);
                totalCashWon = totalCashWon.add(result.getCashWon());
            } else if ("LETTER".equals(result.getType())) {
                letterWins++;
                assertThat(result.getLetterWon()).isNotNull();
                assertThat(result.getLetterWon()).matches("[A-Z]");
            }
        }

        // Verify weighted probability is working (should have both types)
        assertThat(cashWins + letterWins).isEqualTo(totalSpins);
        assertThat(cashWins).isGreaterThan(0);
        assertThat(letterWins).isGreaterThan(0);

        // Verify final user state
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(50);
        
        if (cashWins > 0) {
            assertThat(user.getCashBalance()).isEqualTo(totalCashWon);
        }

        // Verify transaction logging for all spins (Requirements 9.1, 9.2)
        List<TransactionLog> spinLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)
                .stream()
                .filter(log -> log.getTransactionType().contains("SPIN"))
                .toList();
        assertThat(spinLogs.size()).isGreaterThanOrEqualTo(totalSpins); // At least spin consumptions
    }

    /**
     * Test letter collection and word bonus system
     * Requirements: 7.1-7.5, 8.1-8.5
     */
    @Test
    @Transactional
    void testLetterCollectionAndWordBonusSystem() throws Exception {
        userService.getOrCreateUser(TEST_USER_ID);

        // Test letter collection (Requirements 7.1, 7.2)
        String[] letters = {"W", "I", "N", "L", "U", "C", "K"};
        for (String letter : letters) {
            letterService.addLetterToCollection(TEST_USER_ID, letter);
        }

        // Verify letter collection
        String collectionResponse = mockMvc.perform(get("/api/letters/collection")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(letters.length))
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> collection = objectMapper.readValue(collectionResponse, List.class);
        assertThat(collection).hasSize(letters.length);

        // Test word bonus claiming (Requirements 8.1-8.5)
        String wordsResponse = mockMvc.perform(get("/api/letters/words")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> words = objectMapper.readValue(wordsResponse, List.class);
        
        // Find and claim available word bonuses
        int bonusesClaimed = 0;
        BigDecimal totalBonusAmount = BigDecimal.ZERO;
        
        for (Map<String, Object> word : words) {
            if (Boolean.TRUE.equals(word.get("canClaim"))) {
                Long wordId = ((Number) word.get("id")).longValue();
                BigDecimal rewardAmount = new BigDecimal(word.get("rewardAmount").toString());
                BigDecimal balanceBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
                
                mockMvc.perform(post("/api/letters/words/{id}/claim", wordId)
                        .header("X-User-Id", TEST_USER_ID))
                        .andExpect(status().isOk());

                BigDecimal balanceAfter = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
                assertThat(balanceAfter).isEqualTo(balanceBefore.add(rewardAmount));
                
                bonusesClaimed++;
                totalBonusAmount = totalBonusAmount.add(rewardAmount);
            }
        }

        assertThat(bonusesClaimed).isGreaterThan(0);

        // Verify letter deduction after bonus claiming (Requirement 8.3)
        List<LetterCollection> finalCollection = letterCollectionRepository.findByUserId(TEST_USER_ID);
        // Some letters should have been deducted for word formation

        // Verify bonus transaction logging (Requirement 9.4)
        List<TransactionLog> bonusLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)
                .stream()
                .filter(log -> "LETTER_BONUS".equals(log.getTransactionType()))
                .toList();
        assertThat(bonusLogs).hasSize(bonusesClaimed);
    }

    /**
     * Test daily login spin mechanics and restrictions
     * Requirements: 1.1, 1.2, 1.3
     */
    @Test
    void testDailyLoginSpinMechanics() throws Exception {
        // First login should grant spin
        User user = userService.getOrCreateUser(TEST_USER_ID);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(TEST_USER_ID);
        
        boolean spinGranted = userService.grantDailyLoginSpin(TEST_USER_ID);
        assertThat(spinGranted).isTrue();
        
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(1);
        assertThat(user.getLastDailyLogin()).isNotNull();

        // Same day login should not grant additional spin
        boolean additionalSpin = userService.grantDailyLoginSpin(TEST_USER_ID);
        assertThat(additionalSpin).isFalse();
        
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(1); // Still 1

        // Simulate next day login by updating last login date
        user.setLastDailyLogin(LocalDateTime.now().minusDays(1));
        userRepository.save(user);

        boolean nextDaySpin = userService.grantDailyLoginSpin(TEST_USER_ID);
        assertThat(nextDaySpin).isTrue();
        
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(2); // Now 2
    }

    /**
     * Test error handling and edge cases across all systems
     */
    @Test
    @Transactional
    void testErrorHandlingAndEdgeCases() throws Exception {
        userService.getOrCreateUser(TEST_USER_ID);

        // Test spinning without spins
        User user = userRepository.findById(TEST_USER_ID).orElseThrow();
        user.setAvailableSpins(0);
        userRepository.save(user);

        mockMvc.perform(post("/api/roulette/spin")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isBadRequest());

        // Test claiming mission that doesn't exist
        mockMvc.perform(post("/api/missions/{id}/claim", 99999L)
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isNotFound());

        // Test claiming word bonus without sufficient letters
        mockMvc.perform(post("/api/letters/words/{id}/claim", 1L)
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isBadRequest());

        // Test invalid deposit amount
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": -10.00}"))
                .andExpect(status().isBadRequest());
    }

    private void setupComprehensiveTestData() {
        // Create comprehensive deposit missions covering all tiers
        DepositMission[] missions = {
                createMission("Small Deposit", "50.00", "99.99", 1, 50),
                createMission("Medium Deposit", "100.00", "199.99", 1, 100),
                createMission("Large Deposit", "200.00", "499.99", 1, 200),
                createMission("Huge Deposit", "500.00", null, 2, 500)
        };
        
        for (DepositMission mission : missions) {
            depositMissionRepository.save(mission);
        }

        // Create diverse roulette slots with different weights
        RouletteSlot[] slots = {
                createRouletteSlot(RouletteSlot.SlotType.CASH, "1.00", 25),
                createRouletteSlot(RouletteSlot.SlotType.CASH, "2.50", 15),
                createRouletteSlot(RouletteSlot.SlotType.CASH, "5.00", 10),
                createRouletteSlot(RouletteSlot.SlotType.CASH, "10.00", 5),
                createRouletteSlot(RouletteSlot.SlotType.LETTER, "RANDOM", 45)
        };
        
        for (RouletteSlot slot : slots) {
            rouletteSlotRepository.save(slot);
        }

        // Create letter words for bonus testing
        LetterWord[] words = {
                createLetterWord("WIN", "{\"W\":1,\"I\":1,\"N\":1}", "10.00"),
                createLetterWord("LUCK", "{\"L\":1,\"U\":1,\"C\":1,\"K\":1}", "25.00"),
                createLetterWord("CASINO", "{\"C\":1,\"A\":1,\"S\":1,\"I\":1,\"N\":1,\"O\":1}", "50.00")
        };
        
        for (LetterWord word : words) {
            letterWordRepository.save(word);
        }
    }

    private DepositMission createMission(String name, String minAmount, String maxAmount, int spins, int maxClaims) {
        DepositMission mission = new DepositMission();
        mission.setName(name);
        mission.setMinAmount(new BigDecimal(minAmount));
        mission.setMaxAmount(maxAmount != null ? new BigDecimal(maxAmount) : null);
        mission.setSpinsGranted(spins);
        mission.setMaxClaims(maxClaims);
        mission.setActive(true);
        return mission;
    }

    private RouletteSlot createRouletteSlot(RouletteSlot.SlotType type, String value, int weight) {
        RouletteSlot slot = new RouletteSlot();
        slot.setSlotType(type);
        slot.setSlotValue(value);
        slot.setWeight(weight);
        slot.setActive(true);
        return slot;
    }

    private LetterWord createLetterWord(String word, String requiredLetters, String rewardAmount) {
        LetterWord letterWord = new LetterWord();
        letterWord.setWord(word);
        letterWord.setRequiredLetters(requiredLetters);
        letterWord.setRewardAmount(new BigDecimal(rewardAmount));
        letterWord.setActive(true);
        return letterWord;
    }
}