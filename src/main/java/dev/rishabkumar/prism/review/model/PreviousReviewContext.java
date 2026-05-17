package dev.rishabkumar.prism.review.model;

public record PreviousReviewContext(
        String commitSha,
        int score,
        String severity,
        int bugCount,
        int securityCount,
        int performanceCount,
        String recommendation
) {}
