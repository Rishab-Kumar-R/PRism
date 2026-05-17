package dev.rishabkumar.review;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReviewRepository implements PanacheRepository<ReviewRecord> {

    public boolean existsByCommitSha(String repoName, int prNumber, String commitSha) {
        return count("repoName = ?1 and prNumber = ?2 and commitSha = ?3", repoName, prNumber, commitSha) > 0;
    }
}
