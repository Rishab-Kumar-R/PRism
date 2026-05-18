package dev.rishabkumar.prism.admin.model;

public record UpdateLimitsRequest(
        Integer customMonthlyReviewLimit,
        Integer customDailyReviewLimit
) {
}
