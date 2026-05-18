package dev.rishabkumar.prism.ratelimit.model;

public enum Tier {
    FREE(50, 5),
    PRO(2_000, 100),
    TEAM(5_000, 250),
    ENTERPRISE(-1, -1);

    private final int monthlyReviewBudget;
    private final int dailyReviewBudget;

    Tier(int monthlyReviewBudget, int dailyReviewBudget) {
        this.monthlyReviewBudget = monthlyReviewBudget;
        this.dailyReviewBudget = dailyReviewBudget;
    }

    public int monthlyReviewBudget() {
        return monthlyReviewBudget;
    }

    public int dailyReviewBudget() {
        return dailyReviewBudget;
    }

    public boolean isUnlimited() {
        return monthlyReviewBudget < 0;
    }
}
