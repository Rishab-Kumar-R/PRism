package dev.rishabkumar.prism.ratelimit.repository;

import dev.rishabkumar.prism.ratelimit.model.DailyUsage;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class DailyUsageRepository implements PanacheRepository<DailyUsage> {

    public Optional<DailyUsage> findByInstallationDay(long installationId, int year, int month, int day) {
        return find("installationId = ?1 and year = ?2 and month = ?3 and day = ?4",
                installationId, year, month, day).firstResultOptional();
    }

    public DailyUsage getOrCreate(long installationId, int year, int month, int day) {
        return findByInstallationDay(installationId, year, month, day).orElseGet(() -> {
            DailyUsage usage = new DailyUsage(installationId, year, month, day);
            persist(usage);
            return usage;
        });
    }

    public void increment(long installationId, int year, int month, int day) {
        DailyUsage usage = getOrCreate(installationId, year, month, day);
        usage.reviewCount++;
    }
}
