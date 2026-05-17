package dev.rishabkumar.prism.review.repository;

import dev.rishabkumar.prism.review.model.PreviousReviewContext;
import dev.rishabkumar.prism.review.model.ReviewRecord;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ReviewRepositoryTest {

    @Inject
    ReviewRepository reviewRepository;

    @BeforeEach
    @Transactional
    void cleanup() {
        reviewRepository.deleteAll();
    }

    @Test
    @Transactional
    void existsByCommitSha_whenExists_returnsTrue() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha123", "APPROVED", 8));

        assertTrue(reviewRepository.existsByCommitSha("repo/a", 1, "sha123"));
    }

    @Test
    @Transactional
    void existsByCommitSha_whenNotExists_returnsFalse() {
        assertFalse(reviewRepository.existsByCommitSha("repo/a", 1, "sha999"));
    }

    @Test
    @Transactional
    void existsByCommitSha_whenDifferentRepo_returnsFalse() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha123", "APPROVED", 8));

        assertFalse(reviewRepository.existsByCommitSha("repo/b", 1, "sha123"));
    }

    @Test
    @Transactional
    void existsByCommitSha_whenDifferentPrNumber_returnsFalse() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha123", "APPROVED", 8));

        assertFalse(reviewRepository.existsByCommitSha("repo/a", 2, "sha123"));
    }

    @Test
    @Transactional
    void countBySeverity_returnsCorrectCounts() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));
        reviewRepository.persist(buildRecord("repo/a", 2, "sha2", "APPROVED", 9));
        reviewRepository.persist(buildRecord("repo/a", 3, "sha3", "NEEDS_WORK", 4));

        long[] counts = reviewRepository.countBySeverity();
        assertEquals(2, counts[0]);
        assertEquals(1, counts[1]);
    }

    @Test
    @Transactional
    void countBySeverity_whenNoReviews_returnsZero() {
        long[] counts = reviewRepository.countBySeverity();
        assertEquals(0, counts[0]);
        assertEquals(0, counts[1]);
    }

    @Test
    @Transactional
    void averageScore_calculatesCorrectly() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));
        reviewRepository.persist(buildRecord("repo/a", 2, "sha2", "NEEDS_WORK", 4));
        reviewRepository.persist(buildRecord("repo/a", 3, "sha3", "APPROVED", 6));

        assertEquals(6.0, reviewRepository.averageScore(), 0.1);
    }

    @Test
    @Transactional
    void averageScore_whenNoReviews_returnsZero() {
        assertEquals(0.0, reviewRepository.averageScore(), 0.0);
    }

    @Test
    @Transactional
    void totalBugs_sumsAllBugCounts() {
        reviewRepository.persist(buildRecordWithIssues("repo/a", 1, "sha1", 3, 0, 0));
        reviewRepository.persist(buildRecordWithIssues("repo/a", 2, "sha2", 2, 0, 0));

        assertEquals(5, reviewRepository.totalBugs());
    }

    @Test
    @Transactional
    void totalSecurityIssues_sumsAllSecurityCounts() {
        reviewRepository.persist(buildRecordWithIssues("repo/a", 1, "sha1", 0, 2, 0));
        reviewRepository.persist(buildRecordWithIssues("repo/a", 2, "sha2", 0, 1, 0));

        assertEquals(3, reviewRepository.totalSecurityIssues());
    }

    @Test
    @Transactional
    void totalPerformanceIssues_sumsAllPerformanceCounts() {
        reviewRepository.persist(buildRecordWithIssues("repo/a", 1, "sha1", 0, 0, 4));
        reviewRepository.persist(buildRecordWithIssues("repo/a", 2, "sha2", 0, 0, 1));

        assertEquals(5, reviewRepository.totalPerformanceIssues());
    }

    @Test
    @Transactional
    void mostReviewedRepo_returnsRepoWithMostReviews() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));
        reviewRepository.persist(buildRecord("repo/a", 2, "sha2", "APPROVED", 7));
        reviewRepository.persist(buildRecord("repo/b", 1, "sha3", "NEEDS_WORK", 4));

        assertEquals("repo/a", reviewRepository.mostReviewedRepo());
    }

    @Test
    @Transactional
    void mostReviewedRepo_whenNoReviews_returnsNone() {
        assertEquals("none", reviewRepository.mostReviewedRepo());
    }

    @Test
    @Transactional
    void wasRecentlyReviewed_whenReviewedJustNow_returnsTrue() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));

        assertTrue(reviewRepository.wasRecentlyReviewed("repo/a", 1, 60));
    }

    @Test
    @Transactional
    void wasRecentlyReviewed_whenNoReviews_returnsFalse() {
        assertFalse(reviewRepository.wasRecentlyReviewed("repo/a", 1, 60));
    }

    @Test
    @Transactional
    void wasRecentlyReviewed_whenDifferentPr_returnsFalse() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));

        assertFalse(reviewRepository.wasRecentlyReviewed("repo/a", 2, 60));
    }

    @Test
    @Transactional
    void wasRecentlyReviewed_whenOldReview_returnsFalse() {
        ReviewRecord old = buildRecord("repo/a", 1, "sha1", "APPROVED", 8);
        old.setReviewedAt(java.time.LocalDateTime.now().minusSeconds(120));
        reviewRepository.persist(old);

        assertFalse(reviewRepository.wasRecentlyReviewed("repo/a", 1, 60));
    }

    @Test
    @Transactional
    void aggregateStats_whenNoReviews_returnsZeros() {
        Object[] agg = reviewRepository.aggregateStats();
        long total = agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        assertEquals(0L, total);
    }

    @Test
    @Transactional
    void aggregateStats_returnsCorrectValues() {
        reviewRepository.persist(buildRecordWithIssues("repo/a", 1, "sha1", 2, 1, 0));
        reviewRepository.persist(buildRecordWithIssues("repo/a", 2, "sha2", 1, 0, 3));

        Object[] agg = reviewRepository.aggregateStats();
        long total = agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        long bugs = agg[4] != null ? ((Number) agg[4]).longValue() : 0L;
        long security = agg[5] != null ? ((Number) agg[5]).longValue() : 0L;
        long perf = agg[6] != null ? ((Number) agg[6]).longValue() : 0L;

        assertEquals(2L, total);
        assertEquals(3L, bugs);
        assertEquals(1L, security);
        assertEquals(3L, perf);
    }

    @Test
    @Transactional
    void findLatestContextByPr_whenExists_returnsContext() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));

        Optional<PreviousReviewContext> result = reviewRepository.findLatestContextByPr("repo/a", 1);

        assertTrue(result.isPresent());
        assertEquals("sha1", result.get().commitSha());
        assertEquals(8, result.get().score());
        assertEquals("APPROVED", result.get().severity());
    }

    @Test
    @Transactional
    void findLatestContextByPr_whenNotExists_returnsEmpty() {
        Optional<PreviousReviewContext> result = reviewRepository.findLatestContextByPr("repo/x", 999);

        assertFalse(result.isPresent());
    }

    private ReviewRecord buildRecord(String repoName, int prNumber, String commitSha,
                                     String severity, int score) {
        return new ReviewRecord(repoName, prNumber, "PR Title", commitSha,
                severity, score, 0, 0, 0, 0,
                "Fix something", "## Review content");
    }

    private ReviewRecord buildRecordWithIssues(String repoName, int prNumber, String commitSha,
                                               int bugs, int security, int performance) {
        return new ReviewRecord(repoName, prNumber, "PR Title", commitSha,
                "APPROVED", 8, bugs, security, performance, 0,
                "Fix something", "## Review content");
    }
}
