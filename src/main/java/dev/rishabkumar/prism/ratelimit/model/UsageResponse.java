package dev.rishabkumar.prism.ratelimit.model;

import java.time.LocalDate;

public record UsageResponse(
        long installationId,
        String accountName,
        Tier tier,
        long monthlyReviewsUsed,
        long monthlyReviewBudget,
        long dailyReviewsUsed,
        long dailyReviewBudget,
        RateLimitStatus status,
        LocalDate monthlyResetsOn,
        LocalDate dailyResetsOn
) {
    public static UsageResponse from(InstallationConfig config,
                                     long monthlyUsed, long dailyUsed) {
        LocalDate today = LocalDate.now();
        boolean unlimited = config.tier.isUnlimited();

        return new UsageResponse(
                config.installationId,
                config.accountName,
                config.tier,
                monthlyUsed,
                unlimited ? -1 : config.effectiveMonthlyBudget(),
                dailyUsed,
                unlimited ? -1 : config.effectiveDailyBudget(),
                resolveStatus(monthlyUsed, config, dailyUsed, unlimited),
                today.withDayOfMonth(1).plusMonths(1),
                today.plusDays(1)
        );
    }

    private static RateLimitStatus resolveStatus(long monthlyUsed, InstallationConfig config,
                                                 long dailyUsed, boolean unlimited) {
        if (unlimited) return RateLimitStatus.OK;
        long monthly = config.effectiveMonthlyBudget();
        long daily = config.effectiveDailyBudget();
        if (monthlyUsed >= monthly || dailyUsed >= daily) return RateLimitStatus.EXCEEDED;
        if (monthlyUsed >= (long) (monthly * 0.80) || dailyUsed >= (long) (daily * 0.80)) return RateLimitStatus.WARNING;
        return RateLimitStatus.OK;
    }
}
