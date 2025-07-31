package com.casino.roulette.repository;

import com.casino.roulette.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setCashBalance(BigDecimal.valueOf(100.00));
        testUser.setAvailableSpins(5);
        testUser.setFirstDepositBonusUsed(false);
        testUser.setLastDailyLogin(LocalDateTime.now().minusDays(1));
        
        entityManager.persistAndFlush(testUser);
    }

    @Test
    void findById_ShouldReturnUser_WhenUserExists() {
        Optional<User> found = userRepository.findById(1L);
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
        assertThat(found.get().getCashBalance()).isEqualTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void findById_ShouldReturnEmpty_WhenUserDoesNotExist() {
        Optional<User> found = userRepository.findById(999L);
        
        assertThat(found).isEmpty();
    }

    @Test
    void updateCashBalance_ShouldIncreaseBalance_WhenAmountIsPositive() {
        BigDecimal addAmount = BigDecimal.valueOf(50.00);
        
        int updated = userRepository.updateCashBalance(1L, addAmount);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getCashBalance()).isEqualTo(BigDecimal.valueOf(150.00));
    }

    @Test
    void updateCashBalance_ShouldDecreaseBalance_WhenAmountIsNegative() {
        BigDecimal subtractAmount = BigDecimal.valueOf(-25.00);
        
        int updated = userRepository.updateCashBalance(1L, subtractAmount);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getCashBalance()).isEqualTo(BigDecimal.valueOf(75.00));
    }

    @Test
    void updateAvailableSpins_ShouldIncreaseSpins_WhenSpinsIsPositive() {
        int addSpins = 3;
        
        int updated = userRepository.updateAvailableSpins(1L, addSpins);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getAvailableSpins()).isEqualTo(8);
    }

    @Test
    void updateLastDailyLogin_ShouldUpdateTimestamp() {
        LocalDateTime newLoginTime = LocalDateTime.now();
        
        int updated = userRepository.updateLastDailyLogin(1L, newLoginTime);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getLastDailyLogin()).isEqualTo(newLoginTime);
    }

    @Test
    void markFirstDepositBonusUsed_ShouldSetFlagToTrue() {
        int updated = userRepository.markFirstDepositBonusUsed(1L);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getFirstDepositBonusUsed()).isTrue();
    }

    @Test
    void hasSufficientSpins_ShouldReturnTrue_WhenUserHasEnoughSpins() {
        boolean hasSufficient = userRepository.hasSufficientSpins(1L, 3);
        
        assertThat(hasSufficient).isTrue();
    }

    @Test
    void hasSufficientSpins_ShouldReturnFalse_WhenUserDoesNotHaveEnoughSpins() {
        boolean hasSufficient = userRepository.hasSufficientSpins(1L, 10);
        
        assertThat(hasSufficient).isFalse();
    }

    @Test
    void consumeSpins_ShouldReduceSpins_WhenUserHasSufficientSpins() {
        int spinsToConsume = 2;
        
        int updated = userRepository.consumeSpins(1L, spinsToConsume);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getAvailableSpins()).isEqualTo(3);
    }

    @Test
    void consumeSpins_ShouldNotReduceSpins_WhenUserDoesNotHaveSufficientSpins() {
        int spinsToConsume = 10;
        
        int updated = userRepository.consumeSpins(1L, spinsToConsume);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(0);
        
        User updatedUser = entityManager.find(User.class, 1L);
        assertThat(updatedUser.getAvailableSpins()).isEqualTo(5); // Should remain unchanged
    }
}