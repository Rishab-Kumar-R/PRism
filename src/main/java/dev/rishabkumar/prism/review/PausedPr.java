package dev.rishabkumar.prism.review;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
public class PausedPr extends PanacheEntity {

    private String repoName;
    private int prNumber;
    private LocalDateTime pausedAt;

    public PausedPr() {
    }

    public PausedPr(String repoName, int prNumber) {
        this.repoName = repoName;
        this.prNumber = prNumber;
        this.pausedAt = LocalDateTime.now();
    }

    public String getRepoName() {
        return repoName;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }
}
