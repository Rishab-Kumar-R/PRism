package dev.rishabkumar.prism.ratelimit.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(columnList = "installationId", unique = true))
public class InstallationConfig extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public long installationId;

    @Column(nullable = true)
    public String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Tier tier = Tier.FREE;

    @Column(nullable = true)
    public Integer customMonthlyReviewLimit;

    @Column(nullable = true)
    public Integer customDailyReviewLimit;

    @Column(nullable = false)
    public boolean active = true;

    @Column(nullable = false)
    public LocalDateTime installedAt = LocalDateTime.now();

    @Column(nullable = true)
    public LocalDateTime uninstalledAt;

    public InstallationConfig() {
    }

    public InstallationConfig(long installationId, String accountName) {
        this.installationId = installationId;
        this.accountName = accountName;
    }

    public int effectiveMonthlyBudget() {
        if (customMonthlyReviewLimit != null && customMonthlyReviewLimit > 0) {
            return customMonthlyReviewLimit;
        }
        return tier.monthlyReviewBudget();
    }

    public int effectiveDailyBudget() {
        if (customDailyReviewLimit != null && customDailyReviewLimit > 0) {
            return customDailyReviewLimit;
        }
        return tier.dailyReviewBudget();
    }
}
