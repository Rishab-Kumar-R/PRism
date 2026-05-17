package dev.rishabkumar.prism.review.repository;

import dev.rishabkumar.prism.review.model.PausedPr;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PausedPrRepository implements PanacheRepository<PausedPr> {

    public boolean isPaused(String repoName, int prNumber) {
        return count("repoName = ?1 and prNumber = ?2", repoName, prNumber) > 0;
    }

    public void resume(String repoName, int prNumber) {
        delete("repoName = ?1 and prNumber = ?2", repoName, prNumber);
    }
}
