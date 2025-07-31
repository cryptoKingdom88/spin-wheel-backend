package com.casino.roulette.repository;

import com.casino.roulette.entity.RouletteSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouletteSlotRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RouletteSlotRepository rouletteSlotRepository;

    private RouletteSlot cashSlot1;
    private RouletteSlot cashSlot2;
    private RouletteSlot letterSlot1;
    private RouletteSlot letterSlot2;
    private RouletteSlot inactiveSlot;

    @BeforeEach
    void setUp() {
        // Create test data
        cashSlot1 = new RouletteSlot(RouletteSlot.SlotType.CASH, "10.00", 50);
        cashSlot1.setActive(true);
        
        cashSlot2 = new RouletteSlot(RouletteSlot.SlotType.CASH, "25.00", 20);
        cashSlot2.setActive(true);
        
        letterSlot1 = new RouletteSlot(RouletteSlot.SlotType.LETTER, "A", 30);
        letterSlot1.setActive(true);
        
        letterSlot2 = new RouletteSlot(RouletteSlot.SlotType.LETTER, "B", 15);
        letterSlot2.setActive(true);
        
        inactiveSlot = new RouletteSlot(RouletteSlot.SlotType.CASH, "100.00", 5);
        inactiveSlot.setActive(false);

        // Persist test data
        entityManager.persistAndFlush(cashSlot1);
        entityManager.persistAndFlush(cashSlot2);
        entityManager.persistAndFlush(letterSlot1);
        entityManager.persistAndFlush(letterSlot2);
        entityManager.persistAndFlush(inactiveSlot);
    }

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveSlots() {
        // When
        List<RouletteSlot> activeSlots = rouletteSlotRepository.findByActiveTrue();

        // Then
        assertThat(activeSlots).hasSize(4);
        assertThat(activeSlots).containsExactlyInAnyOrder(cashSlot1, cashSlot2, letterSlot1, letterSlot2);
        assertThat(activeSlots).doesNotContain(inactiveSlot);
    }

    @Test
    void findActiveSlotsByWeightDesc_ShouldReturnActiveSlotsOrderedByWeightDescending() {
        // When
        List<RouletteSlot> slots = rouletteSlotRepository.findActiveSlotsByWeightDesc();

        // Then
        assertThat(slots).hasSize(4);
        assertThat(slots.get(0)).isEqualTo(cashSlot1); // weight 50
        assertThat(slots.get(1)).isEqualTo(letterSlot1); // weight 30
        assertThat(slots.get(2)).isEqualTo(cashSlot2); // weight 20
        assertThat(slots.get(3)).isEqualTo(letterSlot2); // weight 15
    }

    @Test
    void findByActiveTrueAndSlotType_ShouldReturnOnlyActiveCashSlots() {
        // When
        List<RouletteSlot> cashSlots = rouletteSlotRepository.findByActiveTrueAndSlotType(RouletteSlot.SlotType.CASH);

        // Then
        assertThat(cashSlots).hasSize(2);
        assertThat(cashSlots).containsExactlyInAnyOrder(cashSlot1, cashSlot2);
    }

    @Test
    void findByActiveTrueAndSlotType_ShouldReturnOnlyActiveLetterSlots() {
        // When
        List<RouletteSlot> letterSlots = rouletteSlotRepository.findByActiveTrueAndSlotType(RouletteSlot.SlotType.LETTER);

        // Then
        assertThat(letterSlots).hasSize(2);
        assertThat(letterSlots).containsExactlyInAnyOrder(letterSlot1, letterSlot2);
    }

    @Test
    void getTotalWeightForActiveSlots_ShouldReturnSumOfActiveSlotWeights() {
        // When
        Long totalWeight = rouletteSlotRepository.getTotalWeightForActiveSlots();

        // Then
        // 50 + 20 + 30 + 15 = 115
        assertThat(totalWeight).isEqualTo(115L);
    }

    @Test
    void getTotalWeightForActiveSlots_WhenNoActiveSlots_ShouldReturnZero() {
        // Given - deactivate all slots
        cashSlot1.setActive(false);
        cashSlot2.setActive(false);
        letterSlot1.setActive(false);
        letterSlot2.setActive(false);
        entityManager.flush();

        // When
        Long totalWeight = rouletteSlotRepository.getTotalWeightForActiveSlots();

        // Then
        assertThat(totalWeight).isEqualTo(0L);
    }

    @Test
    void findActiveSlotsWithMinWeight_ShouldReturnSlotsWithWeightGreaterThanOrEqualToMinWeight() {
        // When
        List<RouletteSlot> slots = rouletteSlotRepository.findActiveSlotsWithMinWeight(25);

        // Then
        assertThat(slots).hasSize(2);
        assertThat(slots.get(0)).isEqualTo(cashSlot1); // weight 50
        assertThat(slots.get(1)).isEqualTo(letterSlot1); // weight 30
    }

    @Test
    void findActiveSlotsWithMinWeight_WhenNoSlotsMatchCriteria_ShouldReturnEmptyList() {
        // When
        List<RouletteSlot> slots = rouletteSlotRepository.findActiveSlotsWithMinWeight(100);

        // Then
        assertThat(slots).isEmpty();
    }

    @Test
    void existsByActiveTrue_WhenActiveSlotsExist_ShouldReturnTrue() {
        // When
        boolean exists = rouletteSlotRepository.existsByActiveTrue();

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByActiveTrue_WhenNoActiveSlotsExist_ShouldReturnFalse() {
        // Given - deactivate all slots
        cashSlot1.setActive(false);
        cashSlot2.setActive(false);
        letterSlot1.setActive(false);
        letterSlot2.setActive(false);
        entityManager.flush();

        // When
        boolean exists = rouletteSlotRepository.existsByActiveTrue();

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void countByActiveTrue_ShouldReturnCorrectCount() {
        // When
        long count = rouletteSlotRepository.countByActiveTrue();

        // Then
        assertThat(count).isEqualTo(4L);
    }

    @Test
    void countByActiveTrueAndSlotType_ShouldReturnCorrectCountForCashSlots() {
        // When
        long count = rouletteSlotRepository.countByActiveTrueAndSlotType(RouletteSlot.SlotType.CASH);

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countByActiveTrueAndSlotType_ShouldReturnCorrectCountForLetterSlots() {
        // When
        long count = rouletteSlotRepository.countByActiveTrueAndSlotType(RouletteSlot.SlotType.LETTER);

        // Then
        assertThat(count).isEqualTo(2L);
    }
}