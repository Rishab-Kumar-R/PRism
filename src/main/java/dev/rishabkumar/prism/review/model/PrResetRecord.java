package dev.rishabkumar.prism.review.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(columnList = "repoName, prNumber"))
public class PrResetRecord extends PanacheEntity {

    public String repoName;
    public int prNumber;
    public LocalDateTime resetAt;

    public PrResetRecord() {
    }

    public PrResetRecord(String repoName, int prNumber) {
        this.repoName = repoName;
        this.prNumber = prNumber;
        this.resetAt = LocalDateTime.now();
    }
}
