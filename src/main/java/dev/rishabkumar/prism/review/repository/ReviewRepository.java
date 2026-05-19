package dev.rishabkumar.prism.review.repository;

import dev.rishabkumar.prism.review.model.PreviousReviewContext;
import dev.rishabkumar.prism.review.model.ReviewRecord;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReviewRepository implements PanacheRepository<ReviewRecord> {

    public boolean existsByCommitSha(String repoName, int prNumber, String commitSha) {
        return count("repoName = ?1 and prNumber = ?2 and commitSha = ?3", repoName, prNumber, commitSha) > 0;
    }

    public boolean wasRecentlyReviewed(String repoName, int prNumber, int cooldownSeconds) {
        return count("repoName = ?1 and prNumber = ?2 and reviewedAt > ?3",
                repoName, prNumber, java.time.LocalDateTime.now().minusSeconds(cooldownSeconds)) > 0;
    }

    /**
     * Aggregate stats in a single JPQL query: total, approved, needsWork, avgScore,
     * totalBugs, totalSecurity, totalPerformance.
     */
    public Object[] aggregateStats() {
        return (Object[]) getEntityManager()
                .createQuery(
                        "SELECT COUNT(r), " +
                        "SUM(CASE WHEN r.severity = 'APPROVED' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.severity = 'NEEDS_WORK' THEN 1 ELSE 0 END), " +
                        "AVG(CASE WHEN r.score > 0 THEN CAST(r.score AS double) ELSE null END), " +
                        "SUM(r.bugCount), " +
                        "SUM(r.securityCount), " +
                        "SUM(r.performanceCount) " +
                        "FROM ReviewRecord r")
                .getSingleResult();
    }

    public long[] countBySeverity() {
        Object[] row = (Object[]) getEntityManager()
                .createQuery(
                        "SELECT " +
                        "SUM(CASE WHEN r.severity = 'APPROVED' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.severity = 'NEEDS_WORK' THEN 1 ELSE 0 END) " +
                        "FROM ReviewRecord r")
                .getSingleResult();
        long approved = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        long needsWork = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        return new long[]{approved, needsWork};
    }

    public double averageScore() {
        Double avg = getEntityManager()
                .createQuery("SELECT AVG(r.score) FROM ReviewRecord r WHERE r.score > 0", Double.class)
                .getSingleResult();
        return avg != null ? avg : 0.0;
    }

    public long totalBugs() {
        Long sum = getEntityManager()
                .createQuery("SELECT SUM(r.bugCount) FROM ReviewRecord r", Long.class)
                .getSingleResult();
        return sum != null ? sum : 0L;
    }

    public long totalSecurityIssues() {
        Long sum = getEntityManager()
                .createQuery("SELECT SUM(r.securityCount) FROM ReviewRecord r", Long.class)
                .getSingleResult();
        return sum != null ? sum : 0L;
    }

    public long totalPerformanceIssues() {
        Long sum = getEntityManager()
                .createQuery("SELECT SUM(r.performanceCount) FROM ReviewRecord r", Long.class)
                .getSingleResult();
        return sum != null ? sum : 0L;
    }

    public String mostReviewedRepo() {
        try {
            return getEntityManager()
                    .createQuery(
                            "SELECT r.repoName FROM ReviewRecord r GROUP BY r.repoName ORDER BY COUNT(r) DESC",
                            String.class)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return "none";
        }
    }

    public Optional<ReviewRecord> findLatestByPr(String repoName, int prNumber) {
        return find("repoName = ?1 and prNumber = ?2 order by reviewedAt desc",
                repoName, prNumber)
                .firstResultOptional();
    }

    /**
     * Projection query - loads only the fields needed for incremental review context,
     * intentionally skipping the large TEXT reviewComment column.
     */
    public Optional<PreviousReviewContext> findLatestContextByPr(String repoName, int prNumber,
                                                                  java.time.LocalDateTime notBefore) {
        try {
            Object[] row = (Object[]) getEntityManager()
                    .createQuery(
                            "SELECT r.commitSha, r.score, r.severity, r.bugCount, " +
                            "r.securityCount, r.performanceCount, r.recommendation " +
                            "FROM ReviewRecord r " +
                            "WHERE r.repoName = :repoName AND r.prNumber = :prNumber " +
                            "AND r.reviewedAt > :notBefore " +
                            "ORDER BY r.reviewedAt DESC")
                    .setParameter("repoName", repoName)
                    .setParameter("prNumber", prNumber)
                    .setParameter("notBefore", notBefore)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(new PreviousReviewContext(
                    (String) row[0],
                    ((Number) row[1]).intValue(),
                    (String) row[2],
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).intValue(),
                    ((Number) row[5]).intValue(),
                    (String) row[6]
            ));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<ReviewRecord> findByRepo(String repoName, int page, int size) {
        return find("repoName = ?1 order by reviewedAt desc", repoName).page(page, size).list();
    }

    public List<ReviewRecord> findByPr(int prNumber, int page, int size) {
        return find("prNumber = ?1 order by reviewedAt desc", prNumber).page(page, size).list();
    }

    public List<ReviewRecord> findByRepoAndPr(String repoName, int prNumber, int page, int size) {
        return find("repoName = ?1 and prNumber = ?2 order by reviewedAt desc", repoName, prNumber)
                .page(page, size).list();
    }

    public List<ReviewRecord> findAllPaged(int page, int size) {
        return find("ORDER BY reviewedAt DESC").page(page, size).list();
    }
}
