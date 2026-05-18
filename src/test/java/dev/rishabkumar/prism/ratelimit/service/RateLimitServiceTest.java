package dev.rishabkumar.prism.ratelimit.service;

import dev.rishabkumar.prism.ratelimit.model.DailyUsage;
import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import dev.rishabkumar.prism.ratelimit.model.MonthlyUsage;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.model.RateLimitStatus;
import dev.rishabkumar.prism.ratelimit.model.Tier;
import dev.rishabkumar.prism.ratelimit.repository.DailyUsageRepository;
import dev.rishabkumar.prism.ratelimit.repository.InstallationConfigRepository;
import dev.rishabkumar.prism.ratelimit.repository.MonthlyUsageRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RateLimitServiceTest {

    @Inject
    RateLimitService rateLimitService;

    @Inject
    InstallationConfigRepository installationConfigRepository;

    @Inject
    MonthlyUsageRepository monthlyUsageRepository;

    @Inject
    DailyUsageRepository dailyUsageRepository;

    private static final long INSTALLATION_ID = 999L;
    private static final String ACCOUNT = "test-org";
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    @Transactional
    void cleanup() {
        dailyUsageRepository.delete("installationId", INSTALLATION_ID);
        monthlyUsageRepository.delete("installationId", INSTALLATION_ID);
        installationConfigRepository.delete("installationId", INSTALLATION_ID);
    }

    @Test
    void check_newInstallation_defaultsToFreeAndReturnsOk() {
        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.OK, result.status());
        assertEquals(Tier.FREE, result.tier());
        assertEquals(0, result.reviewsUsed());
        assertEquals(Tier.FREE.monthlyReviewBudget(), result.reviewBudget());
        assertEquals(0, result.dailyReviewsUsed());
        assertEquals(Tier.FREE.dailyReviewBudget(), result.dailyReviewBudget());
    }

    @Test
    @Transactional
    void check_enterprise_alwaysOkRegardlessOfUsage() {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, ACCOUNT);
        config.tier = Tier.ENTERPRISE;
        installationConfigRepository.persist(config);

        setMonthlyCount(INSTALLATION_ID, 999_999);
        setDailyCount(INSTALLATION_ID, 999_999);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.OK, result.status());
        assertEquals(Tier.ENTERPRISE, result.tier());
    }

    @Test
    @Transactional
    void check_atMonthlyLimit_returnsExceeded() {
        setMonthlyCount(INSTALLATION_ID, Tier.FREE.monthlyReviewBudget());

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.EXCEEDED, result.status());
        assertTrue(result.reviewsUsed() >= result.reviewBudget());
    }

    @Test
    @Transactional
    void check_atDailyLimit_returnsExceeded() {
        setDailyCount(INSTALLATION_ID, Tier.FREE.dailyReviewBudget());

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.EXCEEDED, result.status());
        assertTrue(result.dailyReviewsUsed() >= result.dailyReviewBudget());
    }

    @Test
    @Transactional
    void check_dailyLimitHit_blocksEvenWhenMonthlyHasRoom() {
        setDailyCount(INSTALLATION_ID, Tier.FREE.dailyReviewBudget());
        setMonthlyCount(INSTALLATION_ID, 1);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.EXCEEDED, result.status());
        assertTrue(result.isDailyExceeded());
    }

    @Test
    @Transactional
    void check_monthlyLimitHit_blocksEvenWhenDailyHasRoom() {
        setMonthlyCount(INSTALLATION_ID, Tier.FREE.monthlyReviewBudget());
        setDailyCount(INSTALLATION_ID, 1);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.EXCEEDED, result.status());
        assertFalse(result.isDailyExceeded());
    }

    @Test
    @Transactional
    void check_at80PercentMonthly_returnsWarning() {
        int warningThreshold = (int) (Tier.FREE.monthlyReviewBudget() * 0.80);
        setMonthlyCount(INSTALLATION_ID, warningThreshold);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.WARNING, result.status());
    }

    @Test
    @Transactional
    void check_at80PercentDaily_returnsWarning() {
        int warningThreshold = (int) (Tier.FREE.dailyReviewBudget() * 0.80);
        setDailyCount(INSTALLATION_ID, warningThreshold);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.WARNING, result.status());
    }

    @Test
    @Transactional
    void check_customMonthlyLimit_overridesTierDefault() {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, ACCOUNT);
        config.tier = Tier.FREE;
        config.customMonthlyReviewLimit = 200;
        installationConfigRepository.persist(config);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(200, result.reviewBudget());
    }

    @Test
    @Transactional
    void check_customDailyLimit_overridesTierDefault() {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, ACCOUNT);
        config.tier = Tier.FREE;
        config.customDailyReviewLimit = 20;
        installationConfigRepository.persist(config);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(20, result.dailyReviewBudget());
    }

    @Test
    void record_incrementsBothDailyAndMonthlyCounters() {
        rateLimitService.record(INSTALLATION_ID);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(1, result.reviewsUsed());
        assertEquals(1, result.dailyReviewsUsed());
    }

    @Test
    void record_multipleReviews_accumulatesCorrectly() {
        rateLimitService.record(INSTALLATION_ID);
        rateLimitService.record(INSTALLATION_ID);
        rateLimitService.record(INSTALLATION_ID);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(3, result.reviewsUsed());
        assertEquals(3, result.dailyReviewsUsed());
    }

    @Test
    @Transactional
    void check_usageFromPreviousMonth_doesNotCountTowardsCurrentMonth() {
        MonthlyUsage lastMonth = new MonthlyUsage(INSTALLATION_ID,
                today.minusMonths(1).getYear(), today.minusMonths(1).getMonthValue());
        lastMonth.reviewCount = Tier.FREE.monthlyReviewBudget();
        monthlyUsageRepository.persist(lastMonth);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.OK, result.status());
        assertEquals(0, result.reviewsUsed());
    }

    @Test
    @Transactional
    void check_usageFromYesterday_doesNotCountTowardsToday() {
        DailyUsage yesterday = new DailyUsage(INSTALLATION_ID,
                today.minusDays(1).getYear(),
                today.minusDays(1).getMonthValue(),
                today.minusDays(1).getDayOfMonth());
        yesterday.reviewCount = Tier.FREE.dailyReviewBudget();
        dailyUsageRepository.persist(yesterday);

        RateLimitResult result = rateLimitService.check(INSTALLATION_ID, ACCOUNT);

        assertEquals(RateLimitStatus.OK, result.status());
        assertEquals(0, result.dailyReviewsUsed());
    }

    private void setMonthlyCount(long installationId, long count) {
        MonthlyUsage usage = monthlyUsageRepository.getOrCreate(installationId, today.getYear(), today.getMonthValue());
        usage.reviewCount = count;
    }

    private void setDailyCount(long installationId, long count) {
        DailyUsage usage = dailyUsageRepository.getOrCreate(installationId, today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        usage.reviewCount = count;
    }
}
