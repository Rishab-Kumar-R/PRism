package dev.rishabkumar.prism.ratelimit.service;

import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.model.RateLimitStatus;
import dev.rishabkumar.prism.ratelimit.repository.DailyUsageRepository;
import dev.rishabkumar.prism.ratelimit.repository.InstallationConfigRepository;
import dev.rishabkumar.prism.ratelimit.repository.MonthlyUsageRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;

@ApplicationScoped
public class RateLimitService {

    private static final double WARNING_THRESHOLD = 0.80;

    @Inject
    InstallationConfigRepository installationConfigRepository;

    @Inject
    MonthlyUsageRepository monthlyUsageRepository;

    @Inject
    DailyUsageRepository dailyUsageRepository;

    @Transactional
    public RateLimitResult check(long installationId, String accountName) {
        LocalDate now = LocalDate.now();
        InstallationConfig config = installationConfigRepository.getOrCreateDefault(installationId, accountName);

        if (config.tier.isUnlimited()) {
            return new RateLimitResult(RateLimitStatus.OK, 0, -1, 0, -1, config.tier);
        }

        long monthlyBudget = config.effectiveMonthlyBudget();
        long dailyBudget = config.effectiveDailyBudget();

        long monthlyUsed = monthlyUsageRepository
                .findByInstallationPeriod(installationId, now.getYear(), now.getMonthValue())
                .map(u -> u.reviewCount)
                .orElse(0L);

        long dailyUsed = dailyUsageRepository
                .findByInstallationDay(installationId, now.getYear(), now.getMonthValue(), now.getDayOfMonth())
                .map(u -> u.reviewCount)
                .orElse(0L);

        RateLimitStatus status = resolveStatus(monthlyUsed, monthlyBudget, dailyUsed, dailyBudget);

        Log.debugf("[installation=%d] Rate limit: monthly=%d/%d daily=%d/%d — %s",
                installationId, monthlyUsed, monthlyBudget, dailyUsed, dailyBudget, status);

        return new RateLimitResult(status, monthlyUsed, monthlyBudget, dailyUsed, dailyBudget, config.tier);
    }

    @Transactional
    public void record(long installationId) {
        LocalDate now = LocalDate.now();
        monthlyUsageRepository.increment(installationId, now.getYear(), now.getMonthValue());
        dailyUsageRepository.increment(installationId, now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        Log.debugf("[installation=%d] Review counted towards daily and monthly budget", installationId);
    }

    private RateLimitStatus resolveStatus(long monthlyUsed, long monthlyBudget,
                                          long dailyUsed, long dailyBudget) {
        if (monthlyUsed >= monthlyBudget || dailyUsed >= dailyBudget) {
            return RateLimitStatus.EXCEEDED;
        }
        if (monthlyUsed >= (long) (monthlyBudget * WARNING_THRESHOLD)
                || dailyUsed >= (long) (dailyBudget * WARNING_THRESHOLD)) {
            return RateLimitStatus.WARNING;
        }
        return RateLimitStatus.OK;
    }
}
