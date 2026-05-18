package dev.rishabkumar.prism.ratelimit.repository;

import dev.rishabkumar.prism.ratelimit.model.MonthlyUsage;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class MonthlyUsageRepository implements PanacheRepository<MonthlyUsage> {

    public Optional<MonthlyUsage> findByInstallationPeriod(long installationId, int year, int month) {
        return find("installationId = ?1 and year = ?2 and month = ?3", installationId, year, month)
                .firstResultOptional();
    }

    public MonthlyUsage getOrCreate(long installationId, int year, int month) {
        return findByInstallationPeriod(installationId, year, month).orElseGet(() -> {
            MonthlyUsage usage = new MonthlyUsage(installationId, year, month);
            persist(usage);
            return usage;
        });
    }

    public void increment(long installationId, int year, int month) {
        MonthlyUsage usage = getOrCreate(installationId, year, month);
        usage.reviewCount++;
    }
}
