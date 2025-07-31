package com.casino.roulette.repository;

import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionLogRepository transactionLogRepository;

    private User testUser;
    private TransactionLog depositLog;
    private TransactionLog rouletteWinLog;
    private TransactionLog letterBonusLog;
    private TransactionLog spinConsumedLog;
    private TransactionLog negativeAmountLog;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User(1L);
        testUser.setCashBalance(BigDecimal.valueOf(100.00));
        testUser.setAvailableSpins(5);
        entityManager.persistAndFlush(testUser);

        // Create test transaction logs
        depositLog = TransactionLog.createDepositLog(1L, BigDecimal.valueOf(50.00));
        rouletteWinLog = TransactionLog.createRouletteWinLog(1L, BigDecimal.valueOf(25.00));
        letterBonusLog = TransactionLog.createLetterBonusLog(1L, BigDecimal.valueOf(10.00), "HAPPY");
        spinConsumedLog = TransactionLog.createSpinConsumedLog(1L);
        negativeAmountLog = new TransactionLog(1L, "TEST_DEBIT", BigDecimal.valueOf(-15.00), "Test debit transaction");

        entityManager.persistAndFlush(depositLog);
        entityManager.persistAndFlush(rouletteWinLog);
        entityManager.persistAndFlush(letterBonusLog);
        entityManager.persistAndFlush(spinConsumedLog);
        entityManager.persistAndFlush(negativeAmountLog);
    }

    @Test
    void findByUserId_ShouldReturnAllTransactionsForUser() {
        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserId(1L);

        // Then
        assertThat(transactions).hasSize(5);
        assertThat(transactions).containsExactlyInAnyOrder(
                depositLog, rouletteWinLog, letterBonusLog, spinConsumedLog, negativeAmountLog);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_ShouldReturnTransactionsInDescendingOrder() {
        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdOrderByCreatedAtDesc(1L);

        // Then
        assertThat(transactions).hasSize(5);
        // Verify they are in descending order by creation time
        for (int i = 0; i < transactions.size() - 1; i++) {
            assertThat(transactions.get(i).getCreatedAt())
                    .isAfterOrEqualTo(transactions.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void findByUserIdAndTransactionType_ShouldReturnTransactionsOfSpecificType() {
        // When
        List<TransactionLog> deposits = transactionLogRepository.findByUserIdAndTransactionType(1L, TransactionLog.TYPE_DEPOSIT);

        // Then
        assertThat(deposits).hasSize(1);
        assertThat(deposits.get(0)).isEqualTo(depositLog);
    }

    @Test
    void findByUserIdWithPagination_ShouldReturnPagedResults() {
        // When
        Pageable pageable = PageRequest.of(0, 3);
        Page<TransactionLog> page = transactionLogRepository.findByUserId(1L, pageable);

        // Then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findCashTransactionsByUserId_ShouldReturnOnlyTransactionsWithCashAmounts() {
        // When
        List<TransactionLog> cashTransactions = transactionLogRepository.findCashTransactionsByUserId(1L);

        // Then
        assertThat(cashTransactions).hasSize(4);
        assertThat(cashTransactions).containsExactlyInAnyOrder(
                depositLog, rouletteWinLog, letterBonusLog, negativeAmountLog);
        assertThat(cashTransactions).doesNotContain(spinConsumedLog);
    }

    @Test
    void findCashCreditsByUserId_ShouldReturnOnlyPositiveAmountTransactions() {
        // When
        List<TransactionLog> credits = transactionLogRepository.findCashCreditsByUserId(1L);

        // Then
        assertThat(credits).hasSize(3);
        assertThat(credits).containsExactlyInAnyOrder(depositLog, rouletteWinLog, letterBonusLog);
    }

    @Test
    void findCashDebitsByUserId_ShouldReturnOnlyNegativeAmountTransactions() {
        // When
        List<TransactionLog> debits = transactionLogRepository.findCashDebitsByUserId(1L);

        // Then
        assertThat(debits).hasSize(1);
        assertThat(debits.get(0)).isEqualTo(negativeAmountLog);
    }

    @Test
    void getTotalCashCreditsForUser_ShouldReturnSumOfPositiveAmounts() {
        // When
        BigDecimal totalCredits = transactionLogRepository.getTotalCashCreditsForUser(1L);

        // Then
        // 50.00 + 25.00 + 10.00 = 85.00
        assertThat(totalCredits).isEqualByComparingTo(BigDecimal.valueOf(85.00));
    }

    @Test
    void getTotalCashDebitsForUser_ShouldReturnAbsoluteSumOfNegativeAmounts() {
        // When
        BigDecimal totalDebits = transactionLogRepository.getTotalCashDebitsForUser(1L);

        // Then
        // ABS(-15.00) = 15.00
        assertThat(totalDebits).isEqualByComparingTo(BigDecimal.valueOf(15.00));
    }

    @Test
    void getNetCashAmountForUser_ShouldReturnNetAmount() {
        // When
        BigDecimal netAmount = transactionLogRepository.getNetCashAmountForUser(1L);

        // Then
        // 50.00 + 25.00 + 10.00 - 15.00 = 70.00
        assertThat(netAmount).isEqualByComparingTo(BigDecimal.valueOf(70.00));
    }

    @Test
    void countByUserIdAndTransactionType_ShouldReturnCorrectCount() {
        // When
        long depositCount = transactionLogRepository.countByUserIdAndTransactionType(1L, TransactionLog.TYPE_DEPOSIT);

        // Then
        assertThat(depositCount).isEqualTo(1);
    }

    @Test
    void findRecentTransactionsByUserId_ShouldReturnLimitedResults() {
        // When
        Pageable pageable = PageRequest.of(0, 3);
        List<TransactionLog> recentTransactions = transactionLogRepository.findRecentTransactionsByUserId(1L, pageable);

        // Then
        assertThat(recentTransactions).hasSize(3);
        // Should be ordered by creation date descending
        for (int i = 0; i < recentTransactions.size() - 1; i++) {
            assertThat(recentTransactions.get(i).getCreatedAt())
                    .isAfterOrEqualTo(recentTransactions.get(i + 1).getCreatedAt());
        }
    }

    @Test
    void findByUserIdAndTransactionTypeIn_ShouldReturnTransactionsOfSpecifiedTypes() {
        // When
        List<String> types = Arrays.asList(TransactionLog.TYPE_DEPOSIT, TransactionLog.TYPE_ROULETTE_WIN);
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdAndTransactionTypeIn(1L, types);

        // Then
        assertThat(transactions).hasSize(2);
        assertThat(transactions).containsExactlyInAnyOrder(depositLog, rouletteWinLog);
    }

    @Test
    void findByUserIdAndDescriptionContaining_ShouldReturnMatchingTransactions() {
        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdAndDescriptionContaining(1L, "deposit");

        // Then
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0)).isEqualTo(depositLog);
    }

    @Test
    void findByUserIdAndAmountGreaterThan_ShouldReturnTransactionsAboveThreshold() {
        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdAndAmountGreaterThan(1L, BigDecimal.valueOf(20.00));

        // Then
        assertThat(transactions).hasSize(2);
        assertThat(transactions).containsExactlyInAnyOrder(depositLog, rouletteWinLog);
    }

    @Test
    void findByUserIdAndAmountLessThan_ShouldReturnTransactionsBelowThreshold() {
        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdAndAmountLessThan(1L, BigDecimal.valueOf(20.00));

        // Then
        assertThat(transactions).hasSize(2);
        assertThat(transactions).containsExactlyInAnyOrder(letterBonusLog, negativeAmountLog);
    }

    @Test
    void findByUserIdAndAmountBetween_ShouldReturnTransactionsInRange() {
        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdAndAmountBetween(
                1L, BigDecimal.valueOf(5.00), BigDecimal.valueOf(30.00));

        // Then
        assertThat(transactions).hasSize(2);
        assertThat(transactions).containsExactlyInAnyOrder(rouletteWinLog, letterBonusLog);
    }

    @Test
    void existsByUserId_WhenUserHasTransactions_ShouldReturnTrue() {
        // When
        boolean exists = transactionLogRepository.existsByUserId(1L);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_WhenUserHasNoTransactions_ShouldReturnFalse() {
        // When
        boolean exists = transactionLogRepository.existsByUserId(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findFirstTransactionByUserId_ShouldReturnOldestTransaction() {
        // When
        Pageable pageable = PageRequest.of(0, 1);
        List<TransactionLog> firstTransaction = transactionLogRepository.findFirstTransactionByUserId(1L, pageable);

        // Then
        assertThat(firstTransaction).hasSize(1);
        // Should be the oldest transaction (first created)
        assertThat(firstTransaction.get(0)).isEqualTo(depositLog);
    }

    @Test
    void findLastTransactionByUserId_ShouldReturnNewestTransaction() {
        // When
        Pageable pageable = PageRequest.of(0, 1);
        List<TransactionLog> lastTransaction = transactionLogRepository.findLastTransactionByUserId(1L, pageable);

        // Then
        assertThat(lastTransaction).hasSize(1);
        // Should be the newest transaction (last created)
        assertThat(lastTransaction.get(0)).isEqualTo(negativeAmountLog);
    }

    @Test
    void findByUserIdAndCreatedAtBetween_ShouldReturnTransactionsInDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        List<TransactionLog> transactions = transactionLogRepository.findByUserIdAndCreatedAtBetween(1L, startDate, endDate);

        // Then
        assertThat(transactions).hasSize(5); // All transactions should be within this range
    }

    @Test
    void countByUserIdAndCreatedAtBetween_ShouldReturnCorrectCount() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        // When
        long count = transactionLogRepository.countByUserIdAndCreatedAtBetween(1L, startDate, endDate);

        // Then
        assertThat(count).isEqualTo(5);
    }
}