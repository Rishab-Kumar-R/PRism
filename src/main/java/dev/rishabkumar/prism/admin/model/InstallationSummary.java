package dev.rishabkumar.prism.admin.model;

import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import dev.rishabkumar.prism.ratelimit.model.Tier;

import java.time.LocalDateTime;

public record InstallationSummary(
        long installationId,
        String accountName,
        Tier tier,
        Integer customMonthlyReviewLimit,
        Integer customDailyReviewLimit,
        boolean active,
        LocalDateTime installedAt,
        LocalDateTime uninstalledAt
) {
    public static InstallationSummary from(InstallationConfig config) {
        return new InstallationSummary(
                config.installationId,
                config.accountName,
                config.tier,
                config.customMonthlyReviewLimit,
                config.customDailyReviewLimit,
                config.active,
                config.installedAt,
                config.uninstalledAt
        );
    }
}
