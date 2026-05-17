package dev.rishabkumar.review;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReviewRepository implements PanacheRepository<ReviewRecord> {

    public boolean existsByCommitSha(String repoName, int prNumber, String commitSha) {
        return count("repoName = ?1 and prNumber = ?2 and commitSha = ?3", repoName, prNumber, commitSha) > 0;
    }

    public boolean wasRecentlyReviewed(String repoName, int prNumber, int cooldownSeconds) {
        return count("repoName = ?1 and prNumber = ?2 and reviewedAt > ?3",
                repoName, prNumber, java.time.LocalDateTime.now().minusSeconds(cooldownSeconds)) > 0;
    }

    public long countApproved() {
        return count("severity", "APPROVED");
    }

    public long countNeedsWork() {
        return count("severity", "NEEDS_WORK");
    }

    public double averageScore() {
        return find("score > 0")
                .stream()
                .mapToInt(ReviewRecord::getScore)
                .average()
                .orElse(0.0);
    }

    public long totalBugs() {
        return find("bugCount > 0")
                .stream()
                .mapToInt(ReviewRecord::getBugCount)
                .sum();
    }

    public long totalSecurityIssues() {
        return find("securityCount > 0")
                .stream()
                .mapToInt(ReviewRecord::getSecurityCount)
                .sum();
    }

    public long totalPerformanceIssues() {
        return find("performanceCount > 0")
                .stream()
                .mapToInt(ReviewRecord::getPerformanceCount)
                .sum();
    }

    public String mostReviewedRepo() {
        return listAll()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ReviewRecord::getRepoName,
                        java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("none");
    }
}
