package dev.rishabkumar.prism.review.service;

import dev.rishabkumar.prism.ai.model.CodeReview;
import dev.rishabkumar.prism.ai.service.AIReviewService;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.review.model.ReviewRecord;
import dev.rishabkumar.prism.review.repository.ReviewRepository;
import dev.rishabkumar.prism.review.service.ReviewService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ReviewServiceTest {

    @Inject
    ReviewService reviewService;

    @InjectMock
    GitHubService gitHubService;

    @InjectMock
    AIReviewService aiReviewService;

    @Inject
    ReviewRepository reviewRepository;

    @BeforeEach
    @Transactional
    void cleanup() {
        reviewRepository.deleteAll();
    }

    @Test
    void review_whenNewCommit_postsReviewComment() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("APPROVED", 8);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn("diff content");
        when(aiReviewService.review(eq("diff content"), isNull())).thenReturn(codeReview);

        reviewService.review(pullRequest, repository);

        verify(gitHubService).postReviewComment(pullRequest, "## Full review");
    }

    @Test
    @Transactional
    void review_whenCommitAlreadyReviewed_skipsReview() throws IOException {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha123"));

        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");

        reviewService.review(pullRequest, repository);

        verify(gitHubService, never()).fetchDiff(any());
        verify(aiReviewService, never()).review(anyString(), any());
    }

    @Test
    @Transactional
    void review_whenRecentlyReviewed_skipsReview() throws IOException {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha-old"));

        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha-new");
        GHRepository repository = mock(GHRepository.class);
        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");

        reviewService.review(pullRequest, repository);

        verify(gitHubService, never()).fetchDiff(any());
        verify(aiReviewService, never()).review(anyString(), any());
    }

    @Test
    void review_whenGeminiFails_postsFallbackComment() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any())).thenThrow(new RuntimeException("Gemini down"));

        reviewService.review(pullRequest, repository);

        verify(gitHubService).postReviewComment(pullRequest,
                "AI review is temporarily unavailable. Please try again later.");
    }

    @Test
    @Transactional
    void review_whenSucceeds_persistsAllFieldsInDB() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(42, "My PR", "sha999");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("NEEDS_WORK", 5);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn("diff content");
        when(aiReviewService.review(anyString(), isNull())).thenReturn(codeReview);

        reviewService.review(pullRequest, repository);

        List<ReviewRecord> records = reviewRepository.listAll();
        assertEquals(1, records.size());
        assertEquals("repo/a", records.getFirst().getRepoName());
        assertEquals(42, records.getFirst().getPrNumber());
        assertEquals("sha999", records.getFirst().getCommitSha());
        assertEquals("NEEDS_WORK", records.getFirst().getSeverity());
        assertEquals(5, records.getFirst().getScore());
    }

    @Test
    void review_whenSucceeds_appliesCorrectLabel() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("APPROVED", 9);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn("diff content");
        when(aiReviewService.review(anyString(), isNull())).thenReturn(codeReview);

        reviewService.review(pullRequest, repository);

        verify(gitHubService).applyLabel(pullRequest, "APPROVED", false);
    }

    @Test
    void review_whenDiffTruncated_appliesLargePrLabel() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("APPROVED", 9);
        String truncatedDiff = "x".repeat(100) + "[Diff truncated";

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn(truncatedDiff);
        when(aiReviewService.review(anyString(), isNull())).thenReturn(codeReview);

        reviewService.review(pullRequest, repository);

        verify(gitHubService).applyLabel(pullRequest, "APPROVED", true);
    }

    private GHPullRequest buildPullRequest(int prNumber, String title, String sha) throws IOException {
        GHPullRequest pullRequest = mock(GHPullRequest.class, RETURNS_DEEP_STUBS);
        when(pullRequest.getNumber()).thenReturn(prNumber);
        when(pullRequest.getTitle()).thenReturn(title);
        when(pullRequest.getHead().getSha()).thenReturn(sha);
        return pullRequest;
    }

    private CodeReview buildCodeReview(String severity, int score) {
        return new CodeReview("Summary", score, severity, 1, 0, 0, 2,
                List.of("Issue 1"), "Fix it", "## Full review");
    }

    private ReviewRecord buildRecord(String repoName, int prNumber, String commitSha) {
        return new ReviewRecord(repoName, prNumber, "PR Title", commitSha,
                "APPROVED", 8, 0, 0, 0, 0, "Fix something", "## Review");
    }
}
