package dev.rishabkumar.review;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ReviewRepository implements PanacheRepository<ReviewRecord> {

    public boolean existsByRepoAndPrNumber(String repoName, int prNumber) {
        return count("repoName = ?1 and prNumber = ?2", repoName, prNumber) > 0;
    }
}
