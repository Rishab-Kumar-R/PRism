package dev.rishabkumar.review;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void countApproved_returnsOnlyApprovedReviews() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));
        reviewRepository.persist(buildRecord("repo/a", 2, "sha2", "APPROVED", 9));
        reviewRepository.persist(buildRecord("repo/a", 3, "sha3", "NEEDS_WORK", 4));

        assertEquals(2, reviewRepository.countApproved());
    }

    @Test
    @Transactional
    void countNeedsWork_returnsOnlyNeedsWorkReviews() {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha1", "APPROVED", 8));
        reviewRepository.persist(buildRecord("repo/a", 2, "sha2", "NEEDS_WORK", 3));
        reviewRepository.persist(buildRecord("repo/a", 3, "sha3", "NEEDS_WORK", 5));

        assertEquals(2, reviewRepository.countNeedsWork());
    }

    @Test
    @Transactional
    void countApproved_whenNoReviews_returnsZero() {
        assertEquals(0, reviewRepository.countApproved());
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
