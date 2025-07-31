package com.casino.roulette.repository;

import com.casino.roulette.entity.LetterCollection;
import com.casino.roulette.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LetterCollectionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LetterCollectionRepository letterCollectionRepository;

    private User testUser;
    private LetterCollection letterA;
    private LetterCollection letterB;
    private LetterCollection letterC;
    private LetterCollection zeroCountLetter;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User(1L);
        testUser.setCashBalance(BigDecimal.valueOf(100.00));
        testUser.setAvailableSpins(5);
        entityManager.persistAndFlush(testUser);

        // Create test letter collections
        letterA = new LetterCollection(1L, "A", 3);
        letterB = new LetterCollection(1L, "B", 2);
        letterC = new LetterCollection(1L, "C", 1);
        zeroCountLetter = new LetterCollection(1L, "Z", 0);

        entityManager.persistAndFlush(letterA);
        entityManager.persistAndFlush(letterB);
        entityManager.persistAndFlush(letterC);
        entityManager.persistAndFlush(zeroCountLetter);
    }

    @Test
    void findByUserId_ShouldReturnAllLetterCollectionsForUser() {
        // When
        List<LetterCollection> collections = letterCollectionRepository.findByUserId(1L);

        // Then
        assertThat(collections).hasSize(4);
        assertThat(collections).containsExactlyInAnyOrder(letterA, letterB, letterC, zeroCountLetter);
    }

    @Test
    void findByUserIdWithPositiveCount_ShouldReturnOnlyLettersWithCountGreaterThanZero() {
        // When
        List<LetterCollection> collections = letterCollectionRepository.findByUserIdWithPositiveCount(1L);

        // Then
        assertThat(collections).hasSize(3);
        assertThat(collections).containsExactlyInAnyOrder(letterA, letterB, letterC);
        assertThat(collections).doesNotContain(zeroCountLetter);
    }

    @Test
    void findByUserIdAndLetter_ShouldReturnSpecificLetterCollection() {
        // When
        Optional<LetterCollection> collection = letterCollectionRepository.findByUserIdAndLetter(1L, "A");

        // Then
        assertThat(collection).isPresent();
        assertThat(collection.get()).isEqualTo(letterA);
        assertThat(collection.get().getCount()).isEqualTo(3);
    }

    @Test
    void findByUserIdAndLetter_WhenLetterNotFound_ShouldReturnEmpty() {
        // When
        Optional<LetterCollection> collection = letterCollectionRepository.findByUserIdAndLetter(1L, "X");

        // Then
        assertThat(collection).isEmpty();
    }

    @Test
    void findByUserIdAndLetterIn_ShouldReturnSpecifiedLetters() {
        // When
        List<LetterCollection> collections = letterCollectionRepository.findByUserIdAndLetterIn(1L, Arrays.asList("A", "C", "X"));

        // Then
        assertThat(collections).hasSize(2);
        assertThat(collections).containsExactlyInAnyOrder(letterA, letterC);
    }

    @Test
    void getLetterCountForUser_ShouldReturnCorrectCount() {
        // When
        Integer count = letterCollectionRepository.getLetterCountForUser(1L, "A");

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void getLetterCountForUser_WhenLetterNotFound_ShouldReturnZero() {
        // When
        Integer count = letterCollectionRepository.getLetterCountForUser(1L, "X");

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void hasAtLeastLetterCount_WhenUserHasEnoughLetters_ShouldReturnTrue() {
        // When
        boolean hasEnough = letterCollectionRepository.hasAtLeastLetterCount(1L, "A", 2);

        // Then
        assertThat(hasEnough).isTrue();
    }

    @Test
    void hasAtLeastLetterCount_WhenUserDoesNotHaveEnoughLetters_ShouldReturnFalse() {
        // When
        boolean hasEnough = letterCollectionRepository.hasAtLeastLetterCount(1L, "A", 5);

        // Then
        assertThat(hasEnough).isFalse();
    }

    @Test
    void hasAtLeastLetterCount_WhenLetterNotFound_ShouldReturnFalse() {
        // When
        boolean hasEnough = letterCollectionRepository.hasAtLeastLetterCount(1L, "X", 1);

        // Then
        assertThat(hasEnough).isFalse();
    }

    @Test
    void incrementExistingLetterCount_ShouldIncreaseExistingLetterCount() {
        // When
        int updated = letterCollectionRepository.incrementExistingLetterCount(1L, "A", 2);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updated).isEqualTo(1); // One row updated
        LetterCollection updatedCollection = letterCollectionRepository.findByUserIdAndLetter(1L, "A").orElse(null);
        assertThat(updatedCollection).isNotNull();
        assertThat(updatedCollection.getCount()).isEqualTo(5); // 3 + 2
    }

    @Test
    void incrementExistingLetterCount_WhenLetterNotExists_ShouldNotUpdateAnyRows() {
        // When
        int updated = letterCollectionRepository.incrementExistingLetterCount(1L, "D", 1);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updated).isEqualTo(0); // No rows updated
        LetterCollection newCollection = letterCollectionRepository.findByUserIdAndLetter(1L, "D").orElse(null);
        assertThat(newCollection).isNull();
    }

    @Test
    void decrementLetterCount_ShouldDecreaseLetterCount() {
        // When
        int updated = letterCollectionRepository.decrementLetterCount(1L, "A", 1);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updated).isEqualTo(1); // One row updated
        LetterCollection collection = letterCollectionRepository.findByUserIdAndLetter(1L, "A").orElse(null);
        assertThat(collection).isNotNull();
        assertThat(collection.getCount()).isEqualTo(2); // 3 - 1
    }

    @Test
    void decrementLetterCount_ShouldNotGoBelowZero() {
        // When
        letterCollectionRepository.decrementLetterCount(1L, "A", 5);
        entityManager.flush();
        entityManager.clear();

        // Then
        LetterCollection collection = letterCollectionRepository.findByUserIdAndLetter(1L, "A").orElse(null);
        assertThat(collection).isNotNull();
        assertThat(collection.getCount()).isEqualTo(0); // Should not go below 0
    }

    @Test
    void updateLetterCount_ShouldSetNewCount() {
        // When
        int updated = letterCollectionRepository.updateLetterCount(1L, "A", 10);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updated).isEqualTo(1);
        LetterCollection collection = letterCollectionRepository.findByUserIdAndLetter(1L, "A").orElse(null);
        assertThat(collection).isNotNull();
        assertThat(collection.getCount()).isEqualTo(10);
    }

    @Test
    void deleteZeroCountLettersForUser_ShouldRemoveZeroCountLetters() {
        // When
        int deleted = letterCollectionRepository.deleteZeroCountLettersForUser(1L);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(deleted).isEqualTo(1); // Only zeroCountLetter should be deleted
        List<LetterCollection> remaining = letterCollectionRepository.findByUserId(1L);
        assertThat(remaining).hasSize(3);
        assertThat(remaining).containsExactlyInAnyOrder(letterA, letterB, letterC);
    }

    @Test
    void getTotalLetterCountForUser_ShouldReturnSumOfAllLetterCounts() {
        // When
        Long totalCount = letterCollectionRepository.getTotalLetterCountForUser(1L);

        // Then
        assertThat(totalCount).isEqualTo(6L); // 3 + 2 + 1 + 0 = 6
    }

    @Test
    void getDistinctLettersForUser_ShouldReturnOnlyLettersWithPositiveCount() {
        // When
        List<String> distinctLetters = letterCollectionRepository.getDistinctLettersForUser(1L);

        // Then
        assertThat(distinctLetters).hasSize(3);
        assertThat(distinctLetters).containsExactlyInAnyOrder("A", "B", "C");
        assertThat(distinctLetters).doesNotContain("Z");
    }

    @Test
    void existsByUserId_WhenUserHasLetterCollections_ShouldReturnTrue() {
        // When
        boolean exists = letterCollectionRepository.existsByUserId(1L);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserId_WhenUserHasNoLetterCollections_ShouldReturnFalse() {
        // When
        boolean exists = letterCollectionRepository.existsByUserId(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void countDistinctLettersForUser_ShouldReturnCorrectCount() {
        // When
        long count = letterCollectionRepository.countDistinctLettersForUser(1L);

        // Then
        assertThat(count).isEqualTo(3L); // A, B, C (Z has count 0)
    }
}