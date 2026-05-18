package dev.rishabkumar.prism.ratelimit.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = @Index(columnList = "installationId, usage_year, usage_month", unique = true))
public class MonthlyUsage extends PanacheEntity {

    @Column(nullable = false)
    public long installationId;

    @Column(name = "usage_year", nullable = false)
    public int year;

    @Column(name = "usage_month", nullable = false)
    public int month;

    @Column(nullable = false)
    public long reviewCount = 0;

    public MonthlyUsage() {
    }

    public MonthlyUsage(long installationId, int year, int month) {
        this.installationId = installationId;
        this.year = year;
        this.month = month;
    }
}
