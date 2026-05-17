package dev.rishabkumar.prism.review;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class ReviewRecord extends PanacheEntity {

    private String repoName;
    private int prNumber;
    private String prTitle;
    private String commitSha;
    private String severity;
    private int score;
    private int bugCount;
    private int securityCount;
    private int performanceCount;
    private int codeQualityCount;
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private LocalDateTime reviewedAt;

    public ReviewRecord() {
    }

    public ReviewRecord(String repoName, int prNumber, String prTitle, String commitSha,
                        String severity, int score, int bugCount, int securityCount,
                        int performanceCount, int codeQualityCount,
                        String recommendation, String reviewComment) {
        this.repoName = repoName;
        this.prNumber = prNumber;
        this.prTitle = prTitle;
        this.commitSha = commitSha;
        this.severity = severity;
        this.score = score;
        this.bugCount = bugCount;
        this.securityCount = securityCount;
        this.performanceCount = performanceCount;
        this.codeQualityCount = codeQualityCount;
        this.recommendation = recommendation;
        this.reviewComment = reviewComment;
        this.reviewedAt = LocalDateTime.now();
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(int prNumber) {
        this.prNumber = prNumber;
    }

    public String getPrTitle() {
        return prTitle;
    }

    public void setPrTitle(String prTitle) {
        this.prTitle = prTitle;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBugCount() {
        return bugCount;
    }

    public void setBugCount(int bugCount) {
        this.bugCount = bugCount;
    }

    public int getSecurityCount() {
        return securityCount;
    }

    public void setSecurityCount(int securityCount) {
        this.securityCount = securityCount;
    }

    public int getPerformanceCount() {
        return performanceCount;
    }

    public void setPerformanceCount(int performanceCount) {
        this.performanceCount = performanceCount;
    }

    public int getCodeQualityCount() {
        return codeQualityCount;
    }

    public void setCodeQualityCount(int codeQualityCount) {
        this.codeQualityCount = codeQualityCount;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
