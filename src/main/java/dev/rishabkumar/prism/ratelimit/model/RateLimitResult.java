package dev.rishabkumar.prism.ratelimit.model;

public record RateLimitResult(
        RateLimitStatus status,
        long reviewsUsed,
        long reviewBudget,
        long dailyReviewsUsed,
        long dailyReviewBudget,
        Tier tier
) {
    public boolean isExceeded() {
        return status == RateLimitStatus.EXCEEDED;
    }

    public boolean isDailyExceeded() {
        return dailyReviewBudget > 0 && dailyReviewsUsed >= dailyReviewBudget;
    }

    public int percentUsed() {
        if (reviewBudget <= 0) return 0;
        return (int) ((reviewsUsed * 100L) / reviewBudget);
    }

    public int dailyPercentUsed() {
        if (dailyReviewBudget <= 0) return 0;
        return (int) ((dailyReviewsUsed * 100L) / dailyReviewBudget);
    }
}
