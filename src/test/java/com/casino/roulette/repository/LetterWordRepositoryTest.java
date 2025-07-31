package com.casino.roulette.repository;

import com.casino.roulette.entity.LetterWord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LetterWordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LetterWordRepository letterWordRepository;

    private LetterWord happyWord;
    private LetterWord loveWord;
    private LetterWord joyWord;
    private LetterWord inactiveWord;

    @BeforeEach
    void setUp() {
        // Create test letter words
        Map<String, Integer> happyLetters = new HashMap<>();
        happyLetters.put("H", 1);
        happyLetters.put("A", 1);
        happyLetters.put("P", 2);
        happyLetters.put("Y", 1);
        happyWord = new LetterWord("HAPPY", happyLetters, BigDecimal.valueOf(50.00));
        happyWord.setActive(true);

        Map<String, Integer> loveLetters = new HashMap<>();
        loveLetters.put("L", 1);
        loveLetters.put("O", 1);
        loveLetters.put("V", 1);
        loveLetters.put("E", 1);
        loveWord = new LetterWord("LOVE", loveLetters, BigDecimal.valueOf(25.00));
        loveWord.setActive(true);

        Map<String, Integer> joyLetters = new HashMap<>();
        joyLetters.put("J", 1);
        joyLetters.put("O", 1);
        joyLetters.put("Y", 1);
        joyWord = new LetterWord("JOY", joyLetters, BigDecimal.valueOf(75.00));
        joyWord.setActive(true);

        Map<String, Integer> inactiveLetters = new HashMap<>();
        inactiveLetters.put("T", 1);
        inactiveLetters.put("E", 1);
        inactiveLetters.put("S", 1);
        inactiveLetters.put("T", 1);
        inactiveWord = new LetterWord("TEST", inactiveLetters, BigDecimal.valueOf(100.00));
        inactiveWord.setActive(false);

        entityManager.persistAndFlush(happyWord);
        entityManager.persistAndFlush(loveWord);
        entityManager.persistAndFlush(joyWord);
        entityManager.persistAndFlush(inactiveWord);
    }

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveWords() {
        // When
        List<LetterWord> activeWords = letterWordRepository.findByActiveTrue();

        // Then
        assertThat(activeWords).hasSize(3);
        assertThat(activeWords).containsExactlyInAnyOrder(happyWord, loveWord, joyWord);
        assertThat(activeWords).doesNotContain(inactiveWord);
    }

    @Test
    void findActiveWordsOrderByRewardDesc_ShouldReturnWordsOrderedByRewardDescending() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsOrderByRewardDesc();

        // Then
        assertThat(words).hasSize(3);
        assertThat(words.get(0)).isEqualTo(joyWord); // 75.00
        assertThat(words.get(1)).isEqualTo(happyWord); // 50.00
        assertThat(words.get(2)).isEqualTo(loveWord); // 25.00
    }

    @Test
    void findActiveWordsOrderByRewardAsc_ShouldReturnWordsOrderedByRewardAscending() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsOrderByRewardAsc();

        // Then
        assertThat(words).hasSize(3);
        assertThat(words.get(0)).isEqualTo(loveWord); // 25.00
        assertThat(words.get(1)).isEqualTo(happyWord); // 50.00
        assertThat(words.get(2)).isEqualTo(joyWord); // 75.00
    }

    @Test
    void findByWordAndActiveTrue_ShouldReturnActiveWordByName() {
        // When
        Optional<LetterWord> word = letterWordRepository.findByWordAndActiveTrue("HAPPY");

        // Then
        assertThat(word).isPresent();
        assertThat(word.get()).isEqualTo(happyWord);
    }

    @Test
    void findByWordAndActiveTrue_WhenWordNotFound_ShouldReturnEmpty() {
        // When
        Optional<LetterWord> word = letterWordRepository.findByWordAndActiveTrue("NONEXISTENT");

        // Then
        assertThat(word).isEmpty();
    }

    @Test
    void findByWordAndActiveTrue_WhenWordInactive_ShouldReturnEmpty() {
        // When
        Optional<LetterWord> word = letterWordRepository.findByWordAndActiveTrue("TEST");

        // Then
        assertThat(word).isEmpty();
    }

    @Test
    void findByActiveTrueAndRewardAmountGreaterThanEqual_ShouldReturnWordsWithMinReward() {
        // When
        List<LetterWord> words = letterWordRepository.findByActiveTrueAndRewardAmountGreaterThanEqual(BigDecimal.valueOf(50.00));

        // Then
        assertThat(words).hasSize(2);
        assertThat(words).containsExactlyInAnyOrder(happyWord, joyWord);
    }

    @Test
    void findByActiveTrueAndRewardAmountLessThanEqual_ShouldReturnWordsWithMaxReward() {
        // When
        List<LetterWord> words = letterWordRepository.findByActiveTrueAndRewardAmountLessThanEqual(BigDecimal.valueOf(50.00));

        // Then
        assertThat(words).hasSize(2);
        assertThat(words).containsExactlyInAnyOrder(happyWord, loveWord);
    }

    @Test
    void findByActiveTrueAndRewardAmountBetween_ShouldReturnWordsInRange() {
        // When
        List<LetterWord> words = letterWordRepository.findByActiveTrueAndRewardAmountBetween(
                BigDecimal.valueOf(30.00), BigDecimal.valueOf(60.00));

        // Then
        assertThat(words).hasSize(1);
        assertThat(words).contains(happyWord);
    }

    @Test
    void existsByWordAndActiveTrue_WhenWordExists_ShouldReturnTrue() {
        // When
        boolean exists = letterWordRepository.existsByWordAndActiveTrue("HAPPY");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByWordAndActiveTrue_WhenWordNotExists_ShouldReturnFalse() {
        // When
        boolean exists = letterWordRepository.existsByWordAndActiveTrue("NONEXISTENT");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByWordAndActiveTrue_WhenWordInactive_ShouldReturnFalse() {
        // When
        boolean exists = letterWordRepository.existsByWordAndActiveTrue("TEST");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void countByActiveTrue_ShouldReturnCorrectCount() {
        // When
        long count = letterWordRepository.countByActiveTrue();

        // Then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void getMaxRewardAmount_ShouldReturnHighestReward() {
        // When
        BigDecimal maxReward = letterWordRepository.getMaxRewardAmount();

        // Then
        assertThat(maxReward).isEqualByComparingTo(BigDecimal.valueOf(75.00));
    }

    @Test
    void getMinRewardAmount_ShouldReturnLowestReward() {
        // When
        BigDecimal minReward = letterWordRepository.getMinRewardAmount();

        // Then
        assertThat(minReward).isEqualByComparingTo(BigDecimal.valueOf(25.00));
    }

    @Test
    void getAverageRewardAmount_ShouldReturnCorrectAverage() {
        // When
        BigDecimal avgReward = letterWordRepository.getAverageRewardAmount();

        // Then
        // (75.00 + 50.00 + 25.00) / 3 = 50.00
        assertThat(avgReward).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void getTotalRewardAmount_ShouldReturnSumOfAllRewards() {
        // When
        BigDecimal totalReward = letterWordRepository.getTotalRewardAmount();

        // Then
        // 75.00 + 50.00 + 25.00 = 150.00
        assertThat(totalReward).isEqualByComparingTo(BigDecimal.valueOf(150.00));
    }

    @Test
    void findActiveWordsContainingLetter_ShouldReturnWordsWithSpecificLetter() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsContainingLetter("\"Y\"");

        // Then
        assertThat(words).hasSize(2);
        assertThat(words).containsExactlyInAnyOrder(happyWord, joyWord);
    }

    @Test
    void findActiveWordsByPartialMatch_ShouldReturnMatchingWords() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsByPartialMatch("OV");

        // Then
        assertThat(words).hasSize(1);
        assertThat(words).contains(loveWord);
    }

    @Test
    void findActiveWordsStartingWith_ShouldReturnWordsWithPrefix() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsStartingWith("H");

        // Then
        assertThat(words).hasSize(1);
        assertThat(words).contains(happyWord);
    }

    @Test
    void findActiveWordsByLength_ShouldReturnWordsOfSpecificLength() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsByLength(3);

        // Then
        assertThat(words).hasSize(1);
        assertThat(words).contains(joyWord);
    }

    @Test
    void findActiveWordsByLengthRange_ShouldReturnWordsInLengthRange() {
        // When
        List<LetterWord> words = letterWordRepository.findActiveWordsByLengthRange(3, 4);

        // Then
        assertThat(words).hasSize(2);
        assertThat(words).containsExactlyInAnyOrder(joyWord, loveWord);
    }
}