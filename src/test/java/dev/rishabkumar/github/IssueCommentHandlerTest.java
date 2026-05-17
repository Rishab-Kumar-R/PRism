package dev.rishabkumar.github;

import dev.rishabkumar.review.ReviewService;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@GitHubAppTest
public class IssueCommentHandlerTest {

    @InjectMock
    ReviewService reviewService;

    @Test
    void onReviewComment_onPullRequest_triggersReview() throws IOException {
        given()
                .github(mocks -> {
                    GHPullRequest pullRequest = mock(GHPullRequest.class, RETURNS_DEEP_STUBS);
                    when(mocks.repository("owner/repo").getPullRequest(1)).thenReturn(pullRequest);
                })
                .when().payloadFromClasspath("/github/review-comment.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks ->
                        verify(reviewService, timeout(2000)).review(any(), any())
                );
    }

    @Test
    void onNonReviewComment_doesNotTriggerReview() throws IOException {
        given()
                .github(mocks -> {})
                .when().payloadFromClasspath("/github/non-review-comment.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks ->
                        verify(reviewService, never()).review(any(), any())
                );
    }

    @Test
    void onReviewComment_onRegularIssue_doesNotTriggerReview() throws IOException {
        given()
                .github(mocks -> {})
                .when().payloadFromClasspath("/github/issue-comment.json")
                .event(GHEvent.ISSUE_COMMENT)
                .then().github(mocks ->
                        verify(reviewService, never()).review(any(), any())
                );
    }
}
