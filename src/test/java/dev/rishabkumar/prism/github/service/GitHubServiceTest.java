package dev.rishabkumar.prism.github.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@QuarkusTest
public class GitHubServiceTest {

    @Inject
    GitHubService gitHubService;

    @Test
    void applyLabel_whenApproved_appliesApprovedLabel() throws IOException {
        GHRepository repository = mock(GHRepository.class);
        GHPullRequest pullRequest = mock(GHPullRequest.class);
        when(pullRequest.getRepository()).thenReturn(repository);
        when(pullRequest.getNumber()).thenReturn(1);

        gitHubService.applyLabel(pullRequest, "APPROVED", false);

        verify(repository).createLabel("ai: approved", "0075ca");
        verify(pullRequest).addLabels("ai: approved");
    }

    @Test
    void applyLabel_whenNeedsWork_appliesNeedsWorkLabel() throws IOException {
        GHRepository repository = mock(GHRepository.class);
        GHPullRequest pullRequest = mock(GHPullRequest.class);
        when(pullRequest.getRepository()).thenReturn(repository);
        when(pullRequest.getNumber()).thenReturn(1);

        gitHubService.applyLabel(pullRequest, "NEEDS_WORK", false);

        verify(repository).createLabel("ai: needs-work", "e11d48");
        verify(pullRequest).addLabels("ai: needs-work");
    }

    @Test
    void applyLabel_whenLargePr_appliesLargePrLabelRegardlessOfSeverity() throws IOException {
        GHRepository repository = mock(GHRepository.class);
        GHPullRequest pullRequest = mock(GHPullRequest.class);
        when(pullRequest.getRepository()).thenReturn(repository);
        when(pullRequest.getNumber()).thenReturn(1);

        gitHubService.applyLabel(pullRequest, "APPROVED", true);

        verify(repository).createLabel("ai: large-pr", "FFA500");
        verify(pullRequest).addLabels("ai: large-pr");
    }

    @Test
    void applyLabel_whenLabelAlreadyExists_doesNotThrow() throws IOException {
        GHRepository repository = mock(GHRepository.class);
        GHPullRequest pullRequest = mock(GHPullRequest.class);
        when(pullRequest.getRepository()).thenReturn(repository);
        when(pullRequest.getNumber()).thenReturn(1);

        doThrow(new IOException("Label already exists"))
                .when(repository).createLabel(anyString(), anyString());

        assertDoesNotThrow(() -> gitHubService.applyLabel(pullRequest, "APPROVED", false));
        verify(pullRequest).addLabels("ai: approved");
    }

    @Test
    void postReviewComment_postsCommentOnPullRequest() throws IOException {
        GHPullRequest pullRequest = mock(GHPullRequest.class);
        when(pullRequest.getNumber()).thenReturn(42);

        gitHubService.postReviewComment(pullRequest, "Great code!");

        verify(pullRequest).comment("Great code!");
    }

    @Test
    void getRepoName_returnsFullName() {
        GHRepository repository = mock(GHRepository.class);
        when(repository.getFullName()).thenReturn("Rishab-Kumar-R/my-repo");

        String result = gitHubService.getRepoName(repository);
        assert result.equals("Rishab-Kumar-R/my-repo");
    }
}
