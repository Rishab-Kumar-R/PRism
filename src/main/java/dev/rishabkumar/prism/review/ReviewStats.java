package dev.rishabkumar.prism.review;

public class ReviewStats {

    private long totalReviews;
    private long approved;
    private long needsWork;
    private String approvalRate;
    private double averageScore;
    private long totalBugs;
    private long totalSecurityIssues;
    private long totalPerformanceIssues;
    private String mostCommonIssue;
    private String mostReviewedRepo;

    public ReviewStats(long totalReviews, long approved, long needsWork,
                       double averageScore, long totalBugs, long totalSecurityIssues,
                       long totalPerformanceIssues, String mostReviewedRepo) {
        this.totalReviews = totalReviews;
        this.approved = approved;
        this.needsWork = needsWork;
        this.approvalRate = totalReviews > 0
                ? String.format("%.0f%%", (approved * 100.0 / totalReviews))
                : "0%";
        this.averageScore = Math.round(averageScore * 10.0) / 10.0;
        this.totalBugs = totalBugs;
        this.totalSecurityIssues = totalSecurityIssues;
        this.totalPerformanceIssues = totalPerformanceIssues;
        this.mostReviewedRepo = mostReviewedRepo;

        long maxIssues = Math.max(totalBugs, Math.max(totalSecurityIssues, totalPerformanceIssues));
        if (maxIssues == 0) this.mostCommonIssue = "none";
        else if (maxIssues == totalBugs) this.mostCommonIssue = "bugs";
        else if (maxIssues == totalSecurityIssues) this.mostCommonIssue = "security";
        else this.mostCommonIssue = "performance";
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public long getApproved() {
        return approved;
    }

    public long getNeedsWork() {
        return needsWork;
    }

    public String getApprovalRate() {
        return approvalRate;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public long getTotalBugs() {
        return totalBugs;
    }

    public long getTotalSecurityIssues() {
        return totalSecurityIssues;
    }

    public long getTotalPerformanceIssues() {
        return totalPerformanceIssues;
    }

    public String getMostCommonIssue() {
        return mostCommonIssue;
    }

    public String getMostReviewedRepo() {
        return mostReviewedRepo;
    }
}
