package dev.rishabkumar.github;

import dev.rishabkumar.ai.GeminiReviewService;
import dev.rishabkumar.review.ReviewRecord;
import dev.rishabkumar.review.ReviewRepository;
import io.quarkiverse.githubapp.event.PullRequest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.kohsuke.github.GHEventPayload;

import java.io.IOException;

public class PullRequestHandler {

    @Inject
    GitHubService gitHubService;

    @Inject
    GeminiReviewService geminiReviewService;

    @Inject
    ReviewRepository reviewRepository;

    @Transactional
    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest payload) throws IOException {
        reviewPullRequest(payload);
    }

    @Transactional
    void onPullRequestSynchronized(@PullRequest.Synchronize GHEventPayload.PullRequest payload) throws IOException {
        reviewPullRequest(payload);
    }

    private void reviewPullRequest(GHEventPayload.PullRequest payload) throws IOException {
        var pullRequest = payload.getPullRequest();
        var repository = payload.getRepository();

        try {
            String diff = gitHubService.fetchDiff(pullRequest);
            String review = geminiReviewService.review(diff);

            gitHubService.postReviewComment(pullRequest, review);

            ReviewRecord record = new ReviewRecord(
                    gitHubService.getRepoName(repository),
                    pullRequest.getNumber(),
                    pullRequest.getTitle(),
                    review
            );

            reviewRepository.persist(record);
        } catch (Exception e) {
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
