package dev.rishabkumar.prism.review.repository;

import dev.rishabkumar.prism.review.model.PrResetRecord;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class PrResetRepository implements PanacheRepository<PrResetRecord> {

    public Optional<LocalDateTime> findLatestReset(String repoName, int prNumber) {
        return find("repoName = ?1 and prNumber = ?2 order by resetAt desc", repoName, prNumber)
                .firstResultOptional()
                .map(r -> r.resetAt);
    }
}
