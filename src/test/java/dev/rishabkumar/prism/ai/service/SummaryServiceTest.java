package dev.rishabkumar.prism.ai.service;

import dev.rishabkumar.prism.ai.client.SummaryAI;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.model.RateLimitStatus;
import dev.rishabkumar.prism.ratelimit.model.Tier;
import dev.rishabkumar.prism.ratelimit.service.RateLimitService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
public class SummaryServiceTest {

    @Inject
    SummaryService summaryService;

    @InjectMock
    SummaryAI summaryAI;

    @InjectMock
    GitHubService gitHubService;

    @InjectMock
    RateLimitService rateLimitService;

    private static final long INSTALLATION_ID = 42L;
    private static final String ACCOUNT = "test-org";

    private GHPullRequest pullRequest;
    private GHRepository repository;

    @BeforeEach
    void setup() throws IOException {
        pullRequest = mock(GHPullRequest.class);
        repository = mock(GHRepository.class);
        when(pullRequest.getNumber()).thenReturn(1);
        when(gitHubService.getRepoName(repository)).thenReturn("owner/repo");
        when(rateLimitService.check(anyLong(), anyString())).thenReturn(okLimit());
        doNothing().when(rateLimitService).record(anyLong());
    }

    @Test
    void summarize_whenRateLimitExceededMonthly_postsLimitComment() throws IOException {
        when(rateLimitService.check(INSTALLATION_ID, ACCOUNT)).thenReturn(monthlyExceededLimit());

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubService).postReviewComment(eq(pullRequest), commentCaptor.capture());
        assertTrue(commentCaptor.getValue().contains("Monthly Budget Exhausted"));
        verifyNoInteractions(summaryAI);
    }

    @Test
    void summarize_whenRateLimitExceededDaily_postsDailyLimitComment() throws IOException {
        when(rateLimitService.check(INSTALLATION_ID, ACCOUNT)).thenReturn(dailyExceededLimit());

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubService).postReviewComment(eq(pullRequest), commentCaptor.capture());
        assertTrue(commentCaptor.getValue().contains("Daily Limit Reached"));
        verifyNoInteractions(summaryAI);
    }

    @Test
    void summarize_whenRateLimitExceeded_doesNotCallAIOrRecord() throws IOException {
        when(rateLimitService.check(INSTALLATION_ID, ACCOUNT)).thenReturn(monthlyExceededLimit());

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verifyNoInteractions(summaryAI);
        verify(rateLimitService, never()).record(anyLong());
    }

    @Test
    void summarize_whenDiffIsEmpty_postsFallbackComment() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(gitHubService).postReviewComment(pullRequest, "Could not generate a summary. Please try again.");
        verifyNoInteractions(summaryAI);
    }

    @Test
    void summarize_whenDiffIsNull_postsFallbackComment() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn(null);

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(gitHubService).postReviewComment(pullRequest, "Could not generate a summary. Please try again.");
        verifyNoInteractions(summaryAI);
    }

    @Test
    void summarize_whenDiffIsEmpty_doesNotRecord() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("   ");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(rateLimitService, never()).record(anyLong());
    }

    @Test
    void summarize_whenAIReturnsNull_postsFallbackComment() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize(anyString())).thenReturn(null);

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(gitHubService).postReviewComment(pullRequest, "Could not generate a summary. Please try again.");
    }

    @Test
    void summarize_whenAIReturnsBlank_postsFallbackComment() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize(anyString())).thenReturn("   ");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(gitHubService).postReviewComment(pullRequest, "Could not generate a summary. Please try again.");
    }

    @Test
    void summarize_whenAIReturnsNull_doesNotRecord() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize(anyString())).thenReturn(null);

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(rateLimitService, never()).record(anyLong());
    }

    @Test
    void summarize_whenSucceeds_postsSummaryComment() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize("diff content")).thenReturn("This PR adds a login feature.");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubService).postReviewComment(eq(pullRequest), commentCaptor.capture());
        String comment = commentCaptor.getValue();
        assertTrue(comment.contains("## PR Summary"));
        assertTrue(comment.contains("This PR adds a login feature."));
        assertTrue(comment.contains("Generated by PRism"));
    }

    @Test
    void summarize_whenSucceeds_recordsUsage() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize(anyString())).thenReturn("Summary text.");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        verify(rateLimitService).record(INSTALLATION_ID);
    }

    @Test
    void summarize_whenWarning_appendsWarningToComment() throws IOException {
        when(rateLimitService.check(INSTALLATION_ID, ACCOUNT)).thenReturn(warningLimit());
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize(anyString())).thenReturn("Summary text.");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubService).postReviewComment(eq(pullRequest), commentCaptor.capture());
        assertTrue(commentCaptor.getValue().contains("Usage warning"));
    }

    @Test
    void summarize_whenOk_doesNotAppendWarning() throws IOException {
        when(gitHubService.fetchDiff(pullRequest)).thenReturn("diff content");
        when(summaryAI.summarize(anyString())).thenReturn("Summary text.");

        summaryService.summarize(pullRequest, repository, INSTALLATION_ID, ACCOUNT);

        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitHubService).postReviewComment(eq(pullRequest), commentCaptor.capture());
        assertFalse(commentCaptor.getValue().contains("Usage warning"));
    }

    private RateLimitResult okLimit() {
        return new RateLimitResult(RateLimitStatus.OK, 5, 50, 1, 5, Tier.FREE);
    }

    private RateLimitResult warningLimit() {
        return new RateLimitResult(RateLimitStatus.WARNING, 40, 50, 4, 5, Tier.FREE);
    }

    private RateLimitResult monthlyExceededLimit() {
        return new RateLimitResult(RateLimitStatus.EXCEEDED, 50, 50, 1, 5, Tier.FREE);
    }

    private RateLimitResult dailyExceededLimit() {
        return new RateLimitResult(RateLimitStatus.EXCEEDED, 5, 50, 5, 5, Tier.FREE);
    }
}
