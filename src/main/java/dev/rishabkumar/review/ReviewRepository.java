package dev.rishabkumar.review;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ReviewRepository implements PanacheRepository<ReviewRecord> {

    public List<ReviewRecord> findByRepo(String repoName) {
        return list("repoName", repoName);
    }

    public List<ReviewRecord> findByPrNumber(int prNumber) {
        return list("prNumber", prNumber);
    }
}
