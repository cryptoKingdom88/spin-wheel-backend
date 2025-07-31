package com.casino.roulette.repository;

import com.casino.roulette.entity.DepositMission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DepositMissionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DepositMissionRepository depositMissionRepository;

    private DepositMission mission1;
    private DepositMission mission2;
    private DepositMission mission3;
    private DepositMission inactiveMission;

    @BeforeEach
    void setUp() {
        // $50-$99 tier
        mission1 = new DepositMission();
        mission1.setName("Tier 1 - Small Deposit");
        mission1.setMinAmount(BigDecimal.valueOf(50));
        mission1.setMaxAmount(BigDecimal.valueOf(99.99));
        mission1.setSpinsGranted(1);
        mission1.setMaxClaims(50);
        mission1.setActive(true);
        
        // $100-$199 tier
        mission2 = new DepositMission();
        mission2.setName("Tier 2 - Medium Deposit");
        mission2.setMinAmount(BigDecimal.valueOf(100));
        mission2.setMaxAmount(BigDecimal.valueOf(199.99));
        mission2.setSpinsGranted(1);
        mission2.setMaxClaims(100);
        mission2.setActive(true);
        
        // $500+ tier (no max amount)
        mission3 = new DepositMission();
        mission3.setName("Tier 4 - Large Deposit");
        mission3.setMinAmount(BigDecimal.valueOf(500));
        mission3.setMaxAmount(null);
        mission3.setSpinsGranted(2);
        mission3.setMaxClaims(500);
        mission3.setActive(true);
        
        // Inactive mission
        inactiveMission = new DepositMission();
        inactiveMission.setName("Inactive Mission");
        inactiveMission.setMinAmount(BigDecimal.valueOf(25));
        inactiveMission.setMaxAmount(BigDecimal.valueOf(49.99));
        inactiveMission.setSpinsGranted(1);
        inactiveMission.setMaxClaims(10);
        inactiveMission.setActive(false);
        
        entityManager.persistAndFlush(mission1);
        entityManager.persistAndFlush(mission2);
        entityManager.persistAndFlush(mission3);
        entityManager.persistAndFlush(inactiveMission);
    }

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveMissions() {
        List<DepositMission> activeMissions = depositMissionRepository.findByActiveTrue();
        
        assertThat(activeMissions).hasSize(3);
        assertThat(activeMissions).extracting(DepositMission::getName)
                .containsExactlyInAnyOrder(
                        "Tier 1 - Small Deposit",
                        "Tier 2 - Medium Deposit", 
                        "Tier 4 - Large Deposit"
                );
    }

    @Test
    void findActiveByAmountRange_ShouldReturnMatchingMission_ForSmallDeposit() {
        BigDecimal depositAmount = BigDecimal.valueOf(75);
        
        List<DepositMission> missions = depositMissionRepository.findActiveByAmountRange(depositAmount);
        
        assertThat(missions).hasSize(1);
        assertThat(missions.get(0).getName()).isEqualTo("Tier 1 - Small Deposit");
    }

    @Test
    void findActiveByAmountRange_ShouldReturnMatchingMission_ForMediumDeposit() {
        BigDecimal depositAmount = BigDecimal.valueOf(150);
        
        List<DepositMission> missions = depositMissionRepository.findActiveByAmountRange(depositAmount);
        
        assertThat(missions).hasSize(1);
        assertThat(missions.get(0).getName()).isEqualTo("Tier 2 - Medium Deposit");
    }

    @Test
    void findActiveByAmountRange_ShouldReturnMatchingMission_ForLargeDeposit() {
        BigDecimal depositAmount = BigDecimal.valueOf(1000);
        
        List<DepositMission> missions = depositMissionRepository.findActiveByAmountRange(depositAmount);
        
        assertThat(missions).hasSize(1);
        assertThat(missions.get(0).getName()).isEqualTo("Tier 4 - Large Deposit");
    }

    @Test
    void findActiveByAmountRange_ShouldReturnEmpty_ForAmountBelowMinimum() {
        BigDecimal depositAmount = BigDecimal.valueOf(25);
        
        List<DepositMission> missions = depositMissionRepository.findActiveByAmountRange(depositAmount);
        
        assertThat(missions).isEmpty();
    }

    @Test
    void findByAmountRange_ShouldReturnExactMatch() {
        Optional<DepositMission> mission = depositMissionRepository.findByAmountRange(
                BigDecimal.valueOf(50), BigDecimal.valueOf(99.99));
        
        assertThat(mission).isPresent();
        assertThat(mission.get().getName()).isEqualTo("Tier 1 - Small Deposit");
    }

    @Test
    void findByAmountRange_ShouldReturnExactMatch_ForNullMaxAmount() {
        Optional<DepositMission> mission = depositMissionRepository.findByAmountRange(
                BigDecimal.valueOf(500), null);
        
        assertThat(mission).isPresent();
        assertThat(mission.get().getName()).isEqualTo("Tier 4 - Large Deposit");
    }

    @Test
    void findActiveOrderedByMinAmount_ShouldReturnMissionsInOrder() {
        List<DepositMission> missions = depositMissionRepository.findActiveOrderedByMinAmount();
        
        assertThat(missions).hasSize(3);
        assertThat(missions.get(0).getMinAmount()).isEqualTo(BigDecimal.valueOf(50));
        assertThat(missions.get(1).getMinAmount()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(missions.get(2).getMinAmount()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    void existsActiveForAmount_ShouldReturnTrue_WhenMissionExists() {
        boolean exists = depositMissionRepository.existsActiveForAmount(BigDecimal.valueOf(75));
        
        assertThat(exists).isTrue();
    }

    @Test
    void existsActiveForAmount_ShouldReturnFalse_WhenNoMissionExists() {
        boolean exists = depositMissionRepository.existsActiveForAmount(BigDecimal.valueOf(25));
        
        assertThat(exists).isFalse();
    }

    @Test
    void findByNameAndActiveTrue_ShouldReturnMission_WhenExists() {
        Optional<DepositMission> mission = depositMissionRepository.findByNameAndActiveTrue("Tier 1 - Small Deposit");
        
        assertThat(mission).isPresent();
        assertThat(mission.get().getMinAmount()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    void findByNameAndActiveTrue_ShouldReturnEmpty_WhenInactive() {
        Optional<DepositMission> mission = depositMissionRepository.findByNameAndActiveTrue("Inactive Mission");
        
        assertThat(mission).isEmpty();
    }
}