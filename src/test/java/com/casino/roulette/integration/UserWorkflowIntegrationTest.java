package com.casino.roulette.integration;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.dto.SpinResultDTO;
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

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserWorkflowIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clean up data
        transactionLogRepository.deleteAll();
        letterCollectionRepository.deleteAll();
        userMissionProgressRepository.deleteAll();
        userRepository.deleteAll();
        
        // Set up test data
        setupTestMissions();
        setupTestRouletteSlots();
        setupTestLetterWords();
    }

    @Test
    @Transactional
    void testCompleteUserWorkflow_NewUserJourney() throws Exception {
        // Step 1: New user gets daily login spin
        User user = userService.getOrCreateUser(TEST_USER_ID);
        assertThat(user.getAvailableSpins()).isEqualTo(1); // Daily login spin
        
        // Verify transaction log
        List<TransactionLog> logs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getTransactionType()).isEqualTo("DAILY_LOGIN_SPIN");

        // Step 2: User makes first deposit
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100.00}"))
                .andExpect(status().isOk());

        // Verify first deposit bonus and deposit mission spins
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getFirstDepositBonusUsed()).isTrue();
        assertThat(user.getAvailableSpins()).isGreaterThan(1); // Daily + first deposit + mission spins
        assertThat(user.getCashBalance()).isEqualTo(new BigDecimal("100.00"));

        // Step 3: Get available missions
        mockMvc.perform(get("/api/missions")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Step 4: Claim mission rewards
        List<MissionDTO> missions = missionService.getAvailableMissions(TEST_USER_ID);
        for (MissionDTO mission : missions) {
            if (mission.getCanClaim()) {
                mockMvc.perform(post("/api/missions/{id}/claim", mission.getId())
                        .header("X-User-Id", TEST_USER_ID))
                        .andExpect(status().isOk());
            }
        }

        // Step 5: Spin roulette multiple times
        int initialSpins = user.getAvailableSpins();
        for (int i = 0; i < Math.min(5, initialSpins); i++) {
            mockMvc.perform(post("/api/roulette/spin")
                    .header("X-User-Id", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.remainingSpins").exists());
        }

        // Step 6: Check letter collection
        mockMvc.perform(get("/api/letters/collection")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Step 7: Try to claim word bonus if possible
        mockMvc.perform(get("/api/letters/words")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify transaction logging throughout the workflow
        logs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        assertThat(logs.size()).isGreaterThan(3); // At least daily login, first deposit, deposit, and some spins
        
        // Verify different transaction types exist
        List<String> transactionTypes = logs.stream()
                .map(TransactionLog::getTransactionType)
                .distinct()
                .toList();
        assertThat(transactionTypes).contains("DAILY_LOGIN_SPIN", "DEPOSIT", "FIRST_DEPOSIT_BONUS");
    }    @Test

    @Transactional
    void testDepositProcessingWorkflow() throws Exception {
        // Create user first
        userService.getOrCreateUser(TEST_USER_ID);

        // Test different deposit amounts and their mission rewards
        BigDecimal[] depositAmounts = {
                new BigDecimal("75.00"),   // $50-$99 tier
                new BigDecimal("150.00"),  // $100-$199 tier
                new BigDecimal("300.00"),  // $200-$499 tier
                new BigDecimal("600.00")   // $500+ tier
        };

        for (BigDecimal amount : depositAmounts) {
            int spinsBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getAvailableSpins();
            
            mockMvc.perform(post("/api/deposits")
                    .header("X-User-Id", TEST_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\": " + amount + "}"))
                    .andExpect(status().isOk());

            int spinsAfter = userRepository.findById(TEST_USER_ID).orElseThrow().getAvailableSpins();
            assertThat(spinsAfter).isGreaterThan(spinsBefore);
        }

        // Verify all deposits are logged
        List<TransactionLog> depositLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)
                .stream()
                .filter(log -> "DEPOSIT".equals(log.getTransactionType()))
                .toList();
        assertThat(depositLogs).hasSize(depositAmounts.length);

        // Verify mission progress is tracked
        List<UserMissionProgress> progressList = userMissionProgressRepository.findByUserId(TEST_USER_ID);
        assertThat(progressList).isNotEmpty();
    }

    @Test
    @Transactional
    void testRouletteSpinningWorkflow() throws Exception {
        // Setup user with spins
        User user = userService.getOrCreateUser(TEST_USER_ID);
        user.setAvailableSpins(10);
        userRepository.save(user);

        int totalCashWon = 0;
        int totalLettersWon = 0;

        // Perform multiple spins and track results
        for (int i = 0; i < 5; i++) {
            String response = mockMvc.perform(post("/api/roulette/spin")
                    .header("X-User-Id", TEST_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.remainingSpins").value(10 - i - 1))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            SpinResultDTO result = objectMapper.readValue(response, SpinResultDTO.class);
            
            if ("CASH".equals(result.getType())) {
                totalCashWon++;
                assertThat(result.getCashWon()).isNotNull();
                assertThat(result.getCashWon()).isGreaterThan(BigDecimal.ZERO);
            } else if ("LETTER".equals(result.getType())) {
                totalLettersWon++;
                assertThat(result.getLetterWon()).isNotNull();
                assertThat(result.getLetterWon()).matches("[A-Z]");
            }
        }

        // Verify spin results were processed
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(5);

        // Verify transaction logs for spins
        List<TransactionLog> spinLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)
                .stream()
                .filter(log -> log.getTransactionType().contains("SPIN"))
                .toList();
        assertThat(spinLogs).hasSize(5 + totalCashWon); // 5 spin consumptions + cash wins

        // Verify letter collection if any letters were won
        if (totalLettersWon > 0) {
            List<LetterCollection> letters = letterCollectionRepository.findByUserId(TEST_USER_ID);
            int totalLetterCount = letters.stream().mapToInt(LetterCollection::getCount).sum();
            assertThat(totalLetterCount).isEqualTo(totalLettersWon);
        }
    }

    @Test
    @Transactional
    void testLetterCollectionAndWordBonusWorkflow() throws Exception {
        // Setup user
        userService.getOrCreateUser(TEST_USER_ID);

        // Manually add letters to form a word (e.g., "WIN")
        letterService.addLetterToCollection(TEST_USER_ID, "W");
        letterService.addLetterToCollection(TEST_USER_ID, "I");
        letterService.addLetterToCollection(TEST_USER_ID, "N");

        // Check letter collection
        mockMvc.perform(get("/api/letters/collection")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        // Get available words
        String wordsResponse = mockMvc.perform(get("/api/letters/words")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Find a claimable word and claim it
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> words = objectMapper.readValue(wordsResponse, List.class);
        for (Map<String, Object> word : words) {
            if (Boolean.TRUE.equals(word.get("canClaim"))) {
                Long wordId = ((Number) word.get("id")).longValue();
                
                BigDecimal balanceBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
                
                mockMvc.perform(post("/api/letters/words/{id}/claim", wordId)
                        .header("X-User-Id", TEST_USER_ID))
                        .andExpect(status().isOk());

                BigDecimal balanceAfter = userRepository.findById(TEST_USER_ID).orElseThrow().getCashBalance();
                assertThat(balanceAfter).isGreaterThan(balanceBefore);
                
                // Verify transaction log
                List<TransactionLog> bonusLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)
                        .stream()
                        .filter(log -> "LETTER_BONUS".equals(log.getTransactionType()))
                        .toList();
                assertThat(bonusLogs).hasSize(1);
                break;
            }
        }
    }

    @Test
    @Transactional
    void testMissionClaimingWorkflow() throws Exception {
        // Setup user with deposit to trigger missions
        userService.getOrCreateUser(TEST_USER_ID);
        
        // Make deposit to activate missions
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 200.00}"))
                .andExpect(status().isOk());

        // Get available missions
        String missionsResponse = mockMvc.perform(get("/api/missions")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> missions = objectMapper.readValue(missionsResponse, List.class);
        
        // Claim all available missions
        for (Map<String, Object> mission : missions) {
            if (Boolean.TRUE.equals(mission.get("canClaim"))) {
                Long missionId = ((Number) mission.get("id")).longValue();
                int spinsBefore = userRepository.findById(TEST_USER_ID).orElseThrow().getAvailableSpins();
                
                mockMvc.perform(post("/api/missions/{id}/claim", missionId)
                        .header("X-User-Id", TEST_USER_ID))
                        .andExpect(status().isOk());

                int spinsAfter = userRepository.findById(TEST_USER_ID).orElseThrow().getAvailableSpins();
                assertThat(spinsAfter).isGreaterThan(spinsBefore);
            }
        }

        // Verify mission progress is updated
        List<UserMissionProgress> progressList = userMissionProgressRepository.findByUserId(TEST_USER_ID);
        assertThat(progressList).isNotEmpty();
        
        for (UserMissionProgress progress : progressList) {
            assertThat(progress.getClaimsUsed()).isGreaterThan(0);
            assertThat(progress.getLastClaimDate()).isNotNull();
        }
    }

    @Test
    @Transactional
    void testTransactionLoggingAcrossAllOperations() throws Exception {
        // Setup user
        userService.getOrCreateUser(TEST_USER_ID);

        // Perform various operations
        // 1. Deposit
        mockMvc.perform(post("/api/deposits")
                .header("X-User-Id", TEST_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100.00}"))
                .andExpect(status().isOk());

        // 2. Claim missions
        List<MissionDTO> missions = missionService.getAvailableMissions(TEST_USER_ID);
        for (MissionDTO mission : missions) {
            if (mission.getCanClaim()) {
                missionService.claimMissionReward(TEST_USER_ID, mission.getId());
            }
        }

        // 3. Spin roulette
        User user = userRepository.findById(TEST_USER_ID).orElseThrow();
        if (user.getAvailableSpins() > 0) {
            rouletteService.spinRoulette(TEST_USER_ID);
        }

        // 4. Add letters and claim bonus if possible
        letterService.addLetterToCollection(TEST_USER_ID, "W");
        letterService.addLetterToCollection(TEST_USER_ID, "I");
        letterService.addLetterToCollection(TEST_USER_ID, "N");

        // Verify comprehensive transaction logging
        List<TransactionLog> allLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        assertThat(allLogs).isNotEmpty();

        // Verify different transaction types are logged
        List<String> transactionTypes = allLogs.stream()
                .map(TransactionLog::getTransactionType)
                .distinct()
                .toList();

        assertThat(transactionTypes).contains("DAILY_LOGIN_SPIN", "DEPOSIT");
        
        // Verify all transactions have proper timestamps
        for (TransactionLog log : allLogs) {
            assertThat(log.getCreatedAt()).isNotNull();
            assertThat(log.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(log.getTransactionType()).isNotNull();
        }
    }

    private void setupTestMissions() {
        // Create test deposit missions
        DepositMission mission1 = new DepositMission();
        mission1.setName("Small Deposit");
        mission1.setMinAmount(new BigDecimal("50.00"));
        mission1.setMaxAmount(new BigDecimal("99.99"));
        mission1.setSpinsGranted(1);
        mission1.setMaxClaims(50);
        mission1.setActive(true);
        depositMissionRepository.save(mission1);

        DepositMission mission2 = new DepositMission();
        mission2.setName("Medium Deposit");
        mission2.setMinAmount(new BigDecimal("100.00"));
        mission2.setMaxAmount(new BigDecimal("199.99"));
        mission2.setSpinsGranted(1);
        mission2.setMaxClaims(100);
        mission2.setActive(true);
        depositMissionRepository.save(mission2);

        DepositMission mission3 = new DepositMission();
        mission3.setName("Large Deposit");
        mission3.setMinAmount(new BigDecimal("200.00"));
        mission3.setMaxAmount(new BigDecimal("499.99"));
        mission3.setSpinsGranted(1);
        mission3.setMaxClaims(200);
        mission3.setActive(true);
        depositMissionRepository.save(mission3);

        DepositMission mission4 = new DepositMission();
        mission4.setName("Huge Deposit");
        mission4.setMinAmount(new BigDecimal("500.00"));
        mission4.setMaxAmount(null);
        mission4.setSpinsGranted(2);
        mission4.setMaxClaims(500);
        mission4.setActive(true);
        depositMissionRepository.save(mission4);
    }

    private void setupTestRouletteSlots() {
        // Create test roulette slots
        RouletteSlot cashSlot1 = new RouletteSlot();
        cashSlot1.setSlotType(RouletteSlot.SlotType.CASH);
        cashSlot1.setSlotValue("1.00");
        cashSlot1.setWeight(30);
        cashSlot1.setActive(true);
        rouletteSlotRepository.save(cashSlot1);

        RouletteSlot cashSlot2 = new RouletteSlot();
        cashSlot2.setSlotType(RouletteSlot.SlotType.CASH);
        cashSlot2.setSlotValue("5.00");
        cashSlot2.setWeight(10);
        cashSlot2.setActive(true);
        rouletteSlotRepository.save(cashSlot2);

        RouletteSlot letterSlot = new RouletteSlot();
        letterSlot.setSlotType(RouletteSlot.SlotType.LETTER);
        letterSlot.setSlotValue("RANDOM");
        letterSlot.setWeight(60);
        letterSlot.setActive(true);
        rouletteSlotRepository.save(letterSlot);
    }

    private void setupTestLetterWords() {
        // Create test letter words
        LetterWord word1 = new LetterWord();
        word1.setWord("WIN");
        word1.setRequiredLetters("{\"W\":1,\"I\":1,\"N\":1}");
        word1.setRewardAmount(new BigDecimal("10.00"));
        word1.setActive(true);
        letterWordRepository.save(word1);

        LetterWord word2 = new LetterWord();
        word2.setWord("LUCK");
        word2.setRequiredLetters("{\"L\":1,\"U\":1,\"C\":1,\"K\":1}");
        word2.setRewardAmount(new BigDecimal("25.00"));
        word2.setActive(true);
        letterWordRepository.save(word2);
    }
}