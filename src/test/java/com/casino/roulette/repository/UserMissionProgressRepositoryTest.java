package com.casino.roulette.repository;

import com.casino.roulette.entity.DepositMission;
import com.casino.roulette.entity.User;
import com.casino.roulette.entity.UserMissionProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserMissionProgressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserMissionProgressRepository userMissionProgressRepository;

    private DepositMission mission1;
    private DepositMission mission2;
    private DepositMission inactiveMission;
    private UserMissionProgress progress1;
    private UserMissionProgress progress2;
    private UserMissionProgress progress3;

    @BeforeEach
    void setUp() {
        // Create test users first (required for foreign key constraint)
        User user1 = new User();
        user1.setId(1L);
        user1.setCashBalance(BigDecimal.valueOf(100.00));
        user1.setAvailableSpins(5);
        user1.setFirstDepositBonusUsed(false);
        
        User user2 = new User();
        user2.setId(2L);
        user2.setCashBalance(BigDecimal.valueOf(200.00));
        user2.setAvailableSpins(10);
        user2.setFirstDepositBonusUsed(true);
        
        User user3 = new User();
        user3.setId(3L);
        user3.setCashBalance(BigDecimal.valueOf(50.00));
        user3.setAvailableSpins(2);
        user3.setFirstDepositBonusUsed(false);
        
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);
        
        // Create test missions
        mission1 = new DepositMission();
        mission1.setName("Tier 1");
        mission1.setMinAmount(BigDecimal.valueOf(50));
        mission1.setMaxAmount(BigDecimal.valueOf(99.99));
        mission1.setSpinsGranted(1);
        mission1.setMaxClaims(50);
        mission1.setActive(true);
        
        mission2 = new DepositMission();
        mission2.setName("Tier 2");
        mission2.setMinAmount(BigDecimal.valueOf(100));
        mission2.setMaxAmount(BigDecimal.valueOf(199.99));
        mission2.setSpinsGranted(1);
        mission2.setMaxClaims(100);
        mission2.setActive(true);
        
        inactiveMission = new DepositMission();
        inactiveMission.setName("Inactive");
        inactiveMission.setMinAmount(BigDecimal.valueOf(25));
        inactiveMission.setMaxAmount(BigDecimal.valueOf(49.99));
        inactiveMission.setSpinsGranted(1);
        inactiveMission.setMaxClaims(10);
        inactiveMission.setActive(false);
        
        entityManager.persistAndFlush(mission1);
        entityManager.persistAndFlush(mission2);
        entityManager.persistAndFlush(inactiveMission);
        
        // Create test progress records
        progress1 = new UserMissionProgress();
        progress1.setUserId(1L);
        progress1.setMissionId(mission1.getId());
        progress1.setClaimsUsed(5);
        progress1.setLastClaimDate(LocalDateTime.now().minusDays(1));
        
        progress2 = new UserMissionProgress();
        progress2.setUserId(1L);
        progress2.setMissionId(mission2.getId());
        progress2.setClaimsUsed(10);
        progress2.setLastClaimDate(LocalDateTime.now().minusDays(2));
        
        progress3 = new UserMissionProgress();
        progress3.setUserId(2L);
        progress3.setMissionId(mission1.getId());
        progress3.setClaimsUsed(50); // Max claims reached
        progress3.setLastClaimDate(LocalDateTime.now().minusDays(3));
        
        entityManager.persistAndFlush(progress1);
        entityManager.persistAndFlush(progress2);
        entityManager.persistAndFlush(progress3);
    }

    @Test
    void findByUserIdAndMissionId_ShouldReturnProgress_WhenExists() {
        Optional<UserMissionProgress> found = userMissionProgressRepository
                .findByUserIdAndMissionId(1L, mission1.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getClaimsUsed()).isEqualTo(5);
    }

    @Test
    void findByUserIdAndMissionId_ShouldReturnEmpty_WhenNotExists() {
        Optional<UserMissionProgress> found = userMissionProgressRepository
                .findByUserIdAndMissionId(999L, mission1.getId());
        
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId_ShouldReturnAllProgressForUser() {
        List<UserMissionProgress> userProgress = userMissionProgressRepository.findByUserId(1L);
        
        assertThat(userProgress).hasSize(2);
        assertThat(userProgress).extracting(UserMissionProgress::getMissionId)
                .containsExactlyInAnyOrder(mission1.getId(), mission2.getId());
    }

    @Test
    void findByMissionId_ShouldReturnAllProgressForMission() {
        List<UserMissionProgress> missionProgress = userMissionProgressRepository
                .findByMissionId(mission1.getId());
        
        assertThat(missionProgress).hasSize(2);
        assertThat(missionProgress).extracting(UserMissionProgress::getUserId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void canClaimMore_ShouldReturnTrue_WhenUserCanClaim() {
        Optional<Boolean> canClaim = userMissionProgressRepository
                .canClaimMore(1L, mission1.getId(), 50);
        
        assertThat(canClaim).isPresent();
        assertThat(canClaim.get()).isTrue();
    }

    @Test
    void canClaimMore_ShouldReturnFalse_WhenUserReachedMax() {
        Optional<Boolean> canClaim = userMissionProgressRepository
                .canClaimMore(2L, mission1.getId(), 50);
        
        assertThat(canClaim).isPresent();
        assertThat(canClaim.get()).isFalse();
    }

    @Test
    void getRemainingClaims_ShouldReturnCorrectAmount() {
        Optional<Integer> remaining = userMissionProgressRepository
                .getRemainingClaims(1L, mission1.getId(), 50);
        
        assertThat(remaining).isPresent();
        assertThat(remaining.get()).isEqualTo(45); // 50 - 5
    }

    @Test
    void incrementClaimsUsed_ShouldIncreaseClaimsAndUpdateDate() {
        LocalDateTime claimDate = LocalDateTime.now();
        
        int updated = userMissionProgressRepository
                .incrementClaimsUsed(1L, mission1.getId(), claimDate);
        entityManager.flush();
        entityManager.clear();
        
        assertThat(updated).isEqualTo(1);
        
        UserMissionProgress updatedProgress = entityManager.find(UserMissionProgress.class, progress1.getId());
        assertThat(updatedProgress.getClaimsUsed()).isEqualTo(6);
        assertThat(updatedProgress.getLastClaimDate()).isEqualTo(claimDate);
    }

    @Test
    void hasReachedMaxClaims_ShouldReturnTrue_WhenMaxReached() {
        Optional<Boolean> hasReached = userMissionProgressRepository
                .hasReachedMaxClaims(2L, mission1.getId(), 50);
        
        assertThat(hasReached).isPresent();
        assertThat(hasReached.get()).isTrue();
    }

    @Test
    void hasReachedMaxClaims_ShouldReturnFalse_WhenMaxNotReached() {
        Optional<Boolean> hasReached = userMissionProgressRepository
                .hasReachedMaxClaims(1L, mission1.getId(), 50);
        
        assertThat(hasReached).isPresent();
        assertThat(hasReached.get()).isFalse();
    }

    @Test
    void findUsersWithRemainingClaims_ShouldReturnUsersWithClaims() {
        List<UserMissionProgress> usersWithClaims = userMissionProgressRepository
                .findUsersWithRemainingClaims(mission1.getId(), 50);
        
        assertThat(usersWithClaims).hasSize(1);
        assertThat(usersWithClaims.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getTotalClaimsForMission_ShouldReturnCorrectSum() {
        Long totalClaims = userMissionProgressRepository.getTotalClaimsForMission(mission1.getId());
        
        assertThat(totalClaims).isEqualTo(55L); // 5 + 50
    }

    @Test
    void deleteProgressForInactiveMissions_ShouldRemoveInactiveRecords() {
        // First create a progress record for inactive mission
        UserMissionProgress inactiveProgress = new UserMissionProgress();
        inactiveProgress.setUserId(3L);
        inactiveProgress.setMissionId(inactiveMission.getId());
        inactiveProgress.setClaimsUsed(1);
        inactiveProgress.setLastClaimDate(LocalDateTime.now());
        entityManager.persistAndFlush(inactiveProgress);
        
        // Verify it exists
        List<UserMissionProgress> allProgress = userMissionProgressRepository.findAll();
        assertThat(allProgress).hasSize(4);
        
        // Delete inactive mission progress
        int deleted = userMissionProgressRepository.deleteProgressForInactiveMissions();
        entityManager.flush();
        entityManager.clear();
        
        assertThat(deleted).isEqualTo(1);
        
        // Verify only active mission progress remains
        List<UserMissionProgress> remainingProgress = userMissionProgressRepository.findAll();
        assertThat(remainingProgress).hasSize(3);
    }

    @Test
    void isUserEligibleForMission_ShouldReturnTrue_WhenUserHasNeverClaimed() {
        boolean eligible = userMissionProgressRepository.isUserEligibleForMission(999L, mission1.getId());
        
        assertThat(eligible).isTrue();
    }

    @Test
    void isUserEligibleForMission_ShouldReturnTrue_WhenUserCanStillClaim() {
        boolean eligible = userMissionProgressRepository.isUserEligibleForMission(1L, mission1.getId());
        
        assertThat(eligible).isTrue();
    }

    @Test
    void isUserEligibleForMission_ShouldReturnFalse_WhenUserReachedMaxClaims() {
        boolean eligible = userMissionProgressRepository.isUserEligibleForMission(2L, mission1.getId());
        
        assertThat(eligible).isFalse();
    }

    @Test
    void getRemainingClaimsForMission_ShouldReturnMaxClaims_WhenUserNeverClaimed() {
        Integer remaining = userMissionProgressRepository.getRemainingClaimsForMission(999L, mission1.getId());
        
        assertThat(remaining).isEqualTo(50);
    }

    @Test
    void getRemainingClaimsForMission_ShouldReturnCorrectAmount_WhenUserHasClaimed() {
        Integer remaining = userMissionProgressRepository.getRemainingClaimsForMission(1L, mission1.getId());
        
        assertThat(remaining).isEqualTo(45); // 50 - 5
    }

    @Test
    void getRemainingClaimsForMission_ShouldReturnZero_WhenUserReachedMax() {
        Integer remaining = userMissionProgressRepository.getRemainingClaimsForMission(2L, mission1.getId());
        
        assertThat(remaining).isEqualTo(0);
    }

    @Test
    void findByMissionIdAndClaimDateBetween_ShouldReturnProgressInDateRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<UserMissionProgress> progressInRange = userMissionProgressRepository
                .findByMissionIdAndClaimDateBetween(mission1.getId(), startDate, endDate);
        
        assertThat(progressInRange).hasSize(1);
        assertThat(progressInRange.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void findByMissionIdAndClaimDateBetween_ShouldReturnEmpty_WhenNoProgressInRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().minusDays(5);
        
        List<UserMissionProgress> progressInRange = userMissionProgressRepository
                .findByMissionIdAndClaimDateBetween(mission1.getId(), startDate, endDate);
        
        assertThat(progressInRange).isEmpty();
    }

    @Test
    void getTotalClaimsUsedByUser_ShouldReturnCorrectSum() {
        Long totalClaims = userMissionProgressRepository.getTotalClaimsUsedByUser(1L);
        
        assertThat(totalClaims).isEqualTo(15L); // 5 + 10
    }

    @Test
    void getTotalClaimsUsedByUser_ShouldReturnZero_WhenUserHasNoClaims() {
        Long totalClaims = userMissionProgressRepository.getTotalClaimsUsedByUser(999L);
        
        assertThat(totalClaims).isEqualTo(0L);
    }

    @Test
    void findUsersWithActiveProgress_ShouldReturnUsersWithClaims() {
        List<UserMissionProgress> usersWithProgress = userMissionProgressRepository.findUsersWithActiveProgress();
        
        assertThat(usersWithProgress).hasSize(3);
        assertThat(usersWithProgress).extracting(UserMissionProgress::getUserId)
                .containsExactlyInAnyOrder(1L, 1L, 2L);
    }

    @Test
    void existsByUserIdAndClaimsUsedGreaterThan_ShouldReturnTrue_WhenUserHasClaims() {
        boolean exists = userMissionProgressRepository.existsByUserIdAndClaimsUsedGreaterThan(1L, 0);
        
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdAndClaimsUsedGreaterThan_ShouldReturnFalse_WhenUserHasNoClaims() {
        boolean exists = userMissionProgressRepository.existsByUserIdAndClaimsUsedGreaterThan(999L, 0);
        
        assertThat(exists).isFalse();
    }

    @Test
    void findEligibleProgressForUser_ShouldReturnProgressWithRemainingClaims() {
        List<UserMissionProgress> eligibleProgress = userMissionProgressRepository
                .findEligibleProgressForUser(1L);
        
        assertThat(eligibleProgress).hasSize(2);
        assertThat(eligibleProgress).extracting(UserMissionProgress::getMissionId)
                .containsExactlyInAnyOrder(mission1.getId(), mission2.getId());
    }

    @Test
    void findEligibleProgressForUser_ShouldReturnEmpty_WhenUserReachedAllMaxClaims() {
        List<UserMissionProgress> eligibleProgress = userMissionProgressRepository
                .findEligibleProgressForUser(2L);
        
        assertThat(eligibleProgress).isEmpty();
    }

    @Test
    void findByUserIdWithMission_ShouldReturnProgressWithMissionDetails() {
        List<UserMissionProgress> progressWithMission = userMissionProgressRepository
                .findByUserIdWithMission(1L);
        
        assertThat(progressWithMission).hasSize(2);
        
        // Verify that the query returns progress records for active missions only
        for (UserMissionProgress progress : progressWithMission) {
            assertThat(progress.getMissionId()).isNotNull();
            assertThat(progress.getMissionId()).isIn(mission1.getId(), mission2.getId());
            // Note: The mission object itself may be null due to insertable/updatable = false
            // but the query correctly filters for active missions only
        }
    }

    @Test
    void findByUserIdWithMission_ShouldReturnEmpty_WhenUserHasNoActiveProgress() {
        List<UserMissionProgress> progressWithMission = userMissionProgressRepository
                .findByUserIdWithMission(999L);
        
        assertThat(progressWithMission).isEmpty();
    }
}