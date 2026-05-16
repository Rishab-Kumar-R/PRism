package dev.rishabkumar.github;

import dev.rishabkumar.ai.GeminiReviewService;
import dev.rishabkumar.review.ReviewRecord;
import dev.rishabkumar.review.ReviewRepository;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkus.logging.Log;
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
        String repoName = gitHubService.getRepoName(repository);

        Log.infof("Received review request for PR #%d in %s", pullRequest.getNumber(), repoName);

        if (reviewRepository.existsByRepoAndPrNumber(repoName, pullRequest.getNumber())) {
            Log.infof("Review already exists for PR #%d in %s, skipping", pullRequest.getNumber(), repoName);
            return;
        }

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
            Log.infof("Review completed and persisted for PR #%d in %s", pullRequest.getNumber(), repoName);
        } catch (Exception e) {
            Log.errorf(e, "Failed to review PR #%d in %s", pullRequest.getNumber(), repoName);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
