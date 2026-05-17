package dev.rishabkumar.prism.review.model;

import java.time.LocalDateTime;

public record ReviewSummary(
        Long id,
        String repoName,
        int prNumber,
        String prTitle,
        String commitSha,
        String severity,
        int score,
        int bugCount,
        int securityCount,
        int performanceCount,
        int codeQualityCount,
        String recommendation,
        LocalDateTime reviewedAt
) {
    public ReviewSummary(ReviewRecord r) {
        this(
                r.id,
                r.getRepoName(),
                r.getPrNumber(),
                r.getPrTitle(),
                r.getCommitSha(),
                r.getSeverity(),
                r.getScore(),
                r.getBugCount(),
                r.getSecurityCount(),
                r.getPerformanceCount(),
                r.getCodeQualityCount(),
                r.getRecommendation(),
                r.getReviewedAt()
        );
    }
}
