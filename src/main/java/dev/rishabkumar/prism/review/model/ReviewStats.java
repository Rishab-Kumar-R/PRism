package dev.rishabkumar.prism.review.model;

public record ReviewStats(
        long totalReviews,
        long approved,
        long needsWork,
        String approvalRate,
        double averageScore,
        long totalBugs,
        long totalSecurityIssues,
        long totalPerformanceIssues,
        String mostCommonIssue,
        String mostReviewedRepo
) {
    public ReviewStats(long totalReviews, long approved, long needsWork,
                       double averageScore, long totalBugs, long totalSecurityIssues,
                       long totalPerformanceIssues, String mostReviewedRepo) {
        this(
                totalReviews,
                approved,
                needsWork,
                totalReviews > 0
                        ? String.format("%.0f%%", (approved * 100.0 / totalReviews))
                        : "0%",
                Math.round(averageScore * 10.0) / 10.0,
                totalBugs,
                totalSecurityIssues,
                totalPerformanceIssues,
                computeMostCommonIssue(totalBugs, totalSecurityIssues, totalPerformanceIssues),
                mostReviewedRepo
        );
    }

    private static String computeMostCommonIssue(long totalBugs, long totalSecurityIssues, long totalPerformanceIssues) {
        long maxIssues = Math.max(totalBugs, Math.max(totalSecurityIssues, totalPerformanceIssues));
        if (maxIssues == 0) return "none";
        else if (maxIssues == totalBugs) return "bugs";
        else if (maxIssues == totalSecurityIssues) return "security";
        else return "performance";
    }
}
