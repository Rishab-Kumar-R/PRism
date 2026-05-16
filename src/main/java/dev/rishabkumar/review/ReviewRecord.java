package dev.rishabkumar.review;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class ReviewRecord extends PanacheEntity {

    private String repoName;
    private int prNumber;
    private String prTitle;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private LocalDateTime reviewedAt;

    public ReviewRecord() {
    }

    public ReviewRecord(String repoName, int prNumber, String prTitle, String reviewComment) {
        this.repoName = repoName;
        this.prNumber = prNumber;
        this.prTitle = prTitle;
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
