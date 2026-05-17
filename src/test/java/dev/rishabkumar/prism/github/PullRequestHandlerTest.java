package dev.rishabkumar.prism.github;

import dev.rishabkumar.prism.review.ReviewService;
import io.quarkiverse.githubapp.testing.GitHubAppTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;

import java.io.IOException;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@QuarkusTest
@GitHubAppTest
public class PullRequestHandlerTest {

    @InjectMock
    ReviewService reviewService;

    @Test
    void onPullRequestOpened_triggersReview() throws IOException {
        given()
                .github(mocks -> {})
                .when().payloadFromClasspath("/github/pr-opened.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks ->
                        verify(reviewService, timeout(2000)).review(any(), any())
                );
    }

    @Test
    void onPullRequestSynchronized_triggersReview() throws IOException {
        given()
                .github(mocks -> {})
                .when().payloadFromClasspath("/github/pr-synchronized.json")
                .event(GHEvent.PULL_REQUEST)
                .then().github(mocks ->
                        verify(reviewService, timeout(2000)).review(any(), any())
                );
    }
}
