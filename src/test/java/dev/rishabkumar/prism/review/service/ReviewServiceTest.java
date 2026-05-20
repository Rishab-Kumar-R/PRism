package dev.rishabkumar.prism.review.service;

import dev.rishabkumar.prism.ai.model.CodeReview;
import dev.rishabkumar.prism.ai.model.ReviewOutcome;
import dev.rishabkumar.prism.ai.service.AIReviewService;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.model.RateLimitStatus;
import dev.rishabkumar.prism.ratelimit.model.Tier;
import dev.rishabkumar.prism.ratelimit.service.RateLimitService;
import dev.rishabkumar.prism.review.model.ReviewRecord;
import dev.rishabkumar.prism.review.repository.ReviewRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @InjectMock
    RateLimitService rateLimitService;

    @Inject
    ReviewRepository reviewRepository;

    private static final long INSTALLATION_ID = 123L;
    private static final String ACCOUNT_NAME = "test-org";

    @BeforeEach
    @Transactional
    void cleanup() {
        reviewRepository.deleteAll();
        when(rateLimitService.check(anyLong(), anyString())).thenReturn(okLimit());
        doNothing().when(rateLimitService).record(anyLong());
    }

    @Test
    void review_whenNewCommit_postsReviewComment() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("APPROVED", 8);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn("diff content");
        when(aiReviewService.review(eq("diff content"), isNull())).thenReturn(ReviewOutcome.single(codeReview));

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(gitHubService).postReviewComment(pullRequest, "## Full review");
    }

    @Test
    @Transactional
    void review_whenCommitAlreadyReviewed_skipsReview() throws IOException {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha123"));

        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(gitHubService, never()).fetchDiff(any());
        verify(aiReviewService, never()).review(anyString(), any(), any());
    }

    @Test
    @Transactional
    void review_whenRecentlyReviewed_skipsReview() throws IOException {
        reviewRepository.persist(buildRecord("repo/a", 1, "sha-old"));

        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha-new");
        GHRepository repository = mock(GHRepository.class);
        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(gitHubService, never()).fetchDiff(any());
        verify(aiReviewService, never()).review(anyString(), any(), any());
    }

    @Test
    void review_whenAIFails_postsFallbackComment() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any())).thenThrow(new RuntimeException("AI down"));

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

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
        when(aiReviewService.review(anyString(), isNull(), any())).thenReturn(ReviewOutcome.single(codeReview));

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

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
        when(aiReviewService.review(anyString(), isNull(), any())).thenReturn(ReviewOutcome.single(codeReview));

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(gitHubService).applyLabel(pullRequest, "APPROVED", false);
    }

    @Test
    void review_whenChunked_appliesLargePrLabel() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("APPROVED", 9);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn("diff content");
        when(aiReviewService.review(anyString(), isNull(), any())).thenReturn(ReviewOutcome.chunked(codeReview));

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(gitHubService).applyLabel(pullRequest, "APPROVED", true);
    }

    @Test
    void review_whenRateLimitExceeded_postsLimitComment() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(rateLimitService.check(INSTALLATION_ID, ACCOUNT_NAME)).thenReturn(exceededLimit());

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(aiReviewService, never()).review(anyString(), any(), any());
        verify(gitHubService).postReviewComment(eq(pullRequest), anyString());
    }

    @Test
    void review_whenSucceeds_recordsOneReview() throws IOException {
        GHPullRequest pullRequest = buildPullRequest(1, "Test PR", "sha123");
        GHRepository repository = mock(GHRepository.class);
        CodeReview codeReview = buildCodeReview("APPROVED", 8);

        when(gitHubService.getRepoName(repository)).thenReturn("repo/a");
        when(gitHubService.fetchDiff(any(), isNull())).thenReturn("diff content");
        when(aiReviewService.review(anyString(), isNull(), any())).thenReturn(ReviewOutcome.single(codeReview));

        reviewService.review(pullRequest, repository, INSTALLATION_ID, ACCOUNT_NAME);

        verify(rateLimitService).record(INSTALLATION_ID);
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
                List.of("Issue 1"), "Fix it", "## Full review", List.of());
    }

    private ReviewRecord buildRecord(String repoName, int prNumber, String commitSha) {
        return new ReviewRecord(repoName, prNumber, "PR Title", commitSha,
                "APPROVED", 8, 0, 0, 0, 0, "Fix something", "## Review");
    }

    private RateLimitResult okLimit() {
        return new RateLimitResult(RateLimitStatus.OK, 5, 50, 1, 5, Tier.FREE);
    }

    private RateLimitResult exceededLimit() {
        return new RateLimitResult(RateLimitStatus.EXCEEDED, 50, 50, 5, 5, Tier.FREE);
    }
}
