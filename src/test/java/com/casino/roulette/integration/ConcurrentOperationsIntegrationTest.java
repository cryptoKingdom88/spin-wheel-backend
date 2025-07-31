package com.casino.roulette.integration;

import com.casino.roulette.dto.MissionDTO;
import com.casino.roulette.entity.*;
import com.casino.roulette.repository.*;
import com.casino.roulette.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConcurrentOperationsIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepositMissionRepository depositMissionRepository;

    @Autowired
    private RouletteSlotRepository rouletteSlotRepository;

    @Autowired
    private LetterCollectionRepository letterCollectionRepository;

    @Autowired
    private LetterWordRepository letterWordRepository;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private UserMissionProgressRepository userMissionProgressRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MissionService missionService;

    @Autowired
    private RouletteService rouletteService;

    @Autowired
    private LetterService letterService;

    private static final Long TEST_USER_ID = 1L;
    private static final int THREAD_COUNT = 10;
    private static final int LOAD_TEST_THREAD_COUNT = 50;
    private static final int PERFORMANCE_TEST_ITERATIONS = 100;

    @BeforeEach
    void setUp() {
        // Clean up data
        transactionLogRepository.deleteAll();
        letterCollectionRepository.deleteAll();
        userMissionProgressRepository.deleteAll();
        userRepository.deleteAll();
        
        setupTestData();
    }

    @Test
    void testConcurrentRouletteSpins() throws InterruptedException {
        // Setup user with many spins
        User user = userService.getOrCreateUser(TEST_USER_ID);
        user.setAvailableSpins(THREAD_COUNT * 2);
        userRepository.save(user);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Submit concurrent spin tasks
        for (int i = 0; i < THREAD_COUNT; i++) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    rouletteService.spinRoulette(TEST_USER_ID);
                    return true;
                } catch (Exception e) {
                    return false;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for all tasks to complete
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify results
        int successfulSpins = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    successfulSpins++;
                }
            } catch (Exception e) {
                // Some spins might fail due to insufficient spins
            }
        }

        // Verify user spins were decremented correctly
        user = userRepository.findById(TEST_USER_ID).orElseThrow();
        assertThat(user.getAvailableSpins()).isEqualTo(THREAD_COUNT * 2 - successfulSpins);

        // Verify transaction logs
        List<TransactionLog> spinLogs = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
        long spinConsumptionLogs = spinLogs.stream()
                .filter(log -> "SPIN_CONSUMPTION".equals(log.getTransactionType()))
                .count();
        assertThat(spinConsumptionLogs).isEqualTo(successfulSpins);
    }

    /**
     * Performance test for high-volume roulette spinning
     * Requirements: 5.1, 5.2, 5.3, 5.4
     */
    @Test
    void testHighVolumeRouletteSpinPerformance() throws InterruptedException {
        // Setup multiple users with spins
        List<Long> userIds = new ArrayList<>();
        for (long i = 1; i <= LOAD_TEST_THREAD_COUNT; i++) {
            userIds.add(i);
            User user = userService.getOrCreateUser(i);
            user.setAvailableSpins(PERFORMANCE_TEST_ITERATIONS);
            userRepository.save(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(LOAD_TEST_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(LOAD_TEST_THREAD_COUNT * PERFORMANCE_TEST_ITERATIONS);
        List<Future<Long>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();

        // Submit high volume spin tasks
        for (Long userId : userIds) {
            for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
                Future<Long> future = executor.submit(() -> {
                    try {
                        long operationStart = System.nanoTime();
                        rouletteService.spinRoulette(userId);
                        long operationEnd = System.nanoTime();
                        return operationEnd - operationStart;
                    } catch (Exception e) {
                        return -1L; // Error indicator
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }
        }

        // Wait for all operations to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Collect performance metrics
        List<Long> operationTimes = new ArrayList<>();
        int successfulOperations = 0;
        int failedOperations = 0;

        for (Future<Long> future : futures) {
            try {
                Long operationTime = future.get();
                if (operationTime > 0) {
                    operationTimes.add(operationTime);
                    successfulOperations++;
                } else {
                    failedOperations++;
                }
            } catch (Exception e) {
                failedOperations++;
            }
        }

        // Performance assertions
        int totalOperations = LOAD_TEST_THREAD_COUNT * PERFORMANCE_TEST_ITERATIONS;
        double successRate = (double) successfulOperations / totalOperations * 100;
        double throughput = (double) successfulOperations / (totalTime / 1000.0); // operations per second

        System.out.println("=== Roulette Spin Performance Test Results ===");
        System.out.println("Total Operations: " + totalOperations);
        System.out.println("Successful Operations: " + successfulOperations);
        System.out.println("Failed Operations: " + failedOperations);
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Total Time: " + totalTime + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " ops/sec");

        if (!operationTimes.isEmpty()) {
            operationTimes.sort(Long::compareTo);
            long avgTime = operationTimes.stream().mapToLong(Long::longValue).sum() / operationTimes.size();
            long medianTime = operationTimes.get(operationTimes.size() / 2);
            long p95Time = operationTimes.get((int) (operationTimes.size() * 0.95));
            long p99Time = operationTimes.get((int) (operationTimes.size() * 0.99));

            System.out.println("Average Response Time: " + String.format("%.2f", avgTime / 1_000_000.0) + "ms");
            System.out.println("Median Response Time: " + String.format("%.2f", medianTime / 1_000_000.0) + "ms");
            System.out.println("95th Percentile: " + String.format("%.2f", p95Time / 1_000_000.0) + "ms");
            System.out.println("99th Percentile: " + String.format("%.2f", p99Time / 1_000_000.0) + "ms");
        }

        // Performance requirements
        assertThat(successRate).isGreaterThan(95.0); // At least 95% success rate
        assertThat(throughput).isGreaterThan(10.0); // At least 10 operations per second
        
        // Verify data consistency
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElseThrow();
            assertThat(user.getAvailableSpins()).isGreaterThanOrEqualTo(0);
        }
    }

    /**
     * Database performance test under concurrent operations
     * Requirements: 6.2, 6.3
     */
    @Test
    void testDatabasePerformanceUnderLoad() throws InterruptedException {
        // Setup test data
        List<Long> userIds = new ArrayList<>();
        for (long i = 1; i <= LOAD_TEST_THREAD_COUNT; i++) {
            userIds.add(i);
            User user = userService.getOrCreateUser(i);
            user.setAvailableSpins(10);
            user.setCashBalance(new BigDecimal("100.00"));
            userRepository.save(user);
        }

        ExecutorService executor = Executors.newFixedThreadPool(LOAD_TEST_THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(LOAD_TEST_THREAD_COUNT * 4); // 4 operations per user
        List<Future<Long>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();

        // Submit mixed database operations
        for (Long userId : userIds) {
            // Read operation
            futures.add(executor.submit(() -> {
                try {
                    long opStart = System.nanoTime();
                    userRepository.findById(userId);
                    long opEnd = System.nanoTime();
                    return opEnd - opStart;
                } finally {
                    latch.countDown();
                }
            }));

            // Update operation
            futures.add(executor.submit(() -> {
                try {
                    long opStart = System.nanoTime();
                    userRepository.updateCashBalance(userId, new BigDecimal("1.00"));
                    long opEnd = System.nanoTime();
                    return opEnd - opStart;
                } finally {
                    latch.countDown();
                }
            }));

            // Insert operation (transaction log)
            futures.add(executor.submit(() -> {
                try {
                    long opStart = System.nanoTime();
                    TransactionLog log = TransactionLog.createDepositLog(userId, new BigDecimal("10.00"));
                    transactionLogRepository.save(log);
                    long opEnd = System.nanoTime();
                    return opEnd - opStart;
                } finally {
                    latch.countDown();
                }
            }));

            // Complex query operation
            futures.add(executor.submit(() -> {
                try {
                    long opStart = System.nanoTime();
                    transactionLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
                    long opEnd = System.nanoTime();
                    return opEnd - opStart;
                } finally {
                    latch.countDown();
                }
            }));
        }

        // Wait for all operations to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Collect performance metrics
        List<Long> operationTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            try {
                operationTimes.add(future.get());
            } catch (Exception e) {
                // Skip failed operations
            }
        }

        if (!operationTimes.isEmpty()) {
            operationTimes.sort(Long::compareTo);
            long avgTime = operationTimes.stream().mapToLong(Long::longValue).sum() / operationTimes.size();
            long medianTime = operationTimes.get(operationTimes.size() / 2);
            long p95Time = operationTimes.get((int) (operationTimes.size() * 0.95));

            System.out.println("=== Database Performance Test Results ===");
            System.out.println("Total Operations: " + operationTimes.size());
            System.out.println("Total Time: " + totalTime + "ms");
            System.out.println("Average DB Operation Time: " + String.format("%.2f", avgTime / 1_000_000.0) + "ms");
            System.out.println("Median DB Operation Time: " + String.format("%.2f", medianTime / 1_000_000.0) + "ms");
            System.out.println("95th Percentile: " + String.format("%.2f", p95Time / 1_000_000.0) + "ms");

            // Performance requirements
            assertThat(avgTime / 1_000_000.0).isLessThan(100.0); // Average operation under 100ms
            assertThat(p95Time / 1_000_000.0).isLessThan(500.0); // 95th percentile under 500ms
        }

        // Verify data consistency
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElseThrow();
            assertThat(user).isNotNull();
            assertThat(user.getCashBalance()).isGreaterThanOrEqualTo(new BigDecimal("100.00")); // Should have increased or stayed same
        }
    }

    private void setupTestData() {
        // Create test deposit mission
        DepositMission mission = new DepositMission();
        mission.setName("Test Mission");
        mission.setMinAmount(new BigDecimal("50.00"));
        mission.setMaxAmount(new BigDecimal("199.99"));
        mission.setSpinsGranted(1);
        mission.setMaxClaims(1);
        mission.setActive(true);
        depositMissionRepository.save(mission);

        // Create test roulette slots
        RouletteSlot cashSlot = new RouletteSlot();
        cashSlot.setSlotType(RouletteSlot.SlotType.CASH);
        cashSlot.setSlotValue("1.00");
        cashSlot.setWeight(50);
        cashSlot.setActive(true);
        rouletteSlotRepository.save(cashSlot);

        RouletteSlot letterSlot = new RouletteSlot();
        letterSlot.setSlotType(RouletteSlot.SlotType.LETTER);
        letterSlot.setSlotValue("RANDOM");
        letterSlot.setWeight(50);
        letterSlot.setActive(true);
        rouletteSlotRepository.save(letterSlot);

        // Create test letter word
        LetterWord word = new LetterWord();
        word.setWord("TEST");
        word.setRequiredLetters("{\"T\":1,\"E\":1,\"S\":1}");
        word.setRewardAmount(new BigDecimal("5.00"));
        word.setActive(true);
        letterWordRepository.save(word);
    }
}