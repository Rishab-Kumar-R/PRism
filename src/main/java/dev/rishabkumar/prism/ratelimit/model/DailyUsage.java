package dev.rishabkumar.prism.ratelimit.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(indexes = @Index(columnList = "installationId, usage_year, usage_month, usage_day", unique = true))
public class DailyUsage extends PanacheEntity {

    @Column(nullable = false)
    public long installationId;

    @Column(name = "usage_year", nullable = false)
    public int year;

    @Column(name = "usage_month", nullable = false)
    public int month;

    @Column(name = "usage_day", nullable = false)
    public int day;

    @Column(nullable = false)
    public long reviewCount = 0;

    public DailyUsage() {
    }

    public DailyUsage(long installationId, int year, int month, int day) {
        this.installationId = installationId;
        this.year = year;
        this.month = month;
        this.day = day;
    }
}
