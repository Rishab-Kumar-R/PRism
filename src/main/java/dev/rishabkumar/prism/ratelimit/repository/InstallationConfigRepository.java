package dev.rishabkumar.prism.ratelimit.repository;

import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class InstallationConfigRepository implements PanacheRepository<InstallationConfig> {

    public Optional<InstallationConfig> findByInstallation(long installationId) {
        return find("installationId", installationId).firstResultOptional();
    }

    public InstallationConfig getOrCreateDefault(long installationId, String accountName) {
        return findByInstallation(installationId).orElseGet(() -> {
            InstallationConfig config = new InstallationConfig(installationId, accountName);
            persist(config);
            return config;
        });
    }

    public void deactivate(long installationId) {
        findByInstallation(installationId).ifPresent(config -> {
            config.active = false;
            config.uninstalledAt = LocalDateTime.now();
        });
    }
}
