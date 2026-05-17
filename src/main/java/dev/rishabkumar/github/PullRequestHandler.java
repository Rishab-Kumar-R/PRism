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
        String commitSha = pullRequest.getHead().getSha();

        Log.infof("Received review request for PR #%d in %s at commit %s", pullRequest.getNumber(), repoName, commitSha);

        if (reviewRepository.existsByCommitSha(repoName, pullRequest.getNumber(), commitSha)) {
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
                    commitSha,
                    review
            );

            reviewRepository.persist(record);
            Log.infof("Review completed for commit %s on PR #%d", commitSha, pullRequest.getNumber());
        } catch (Exception e) {
            Log.errorf(e, "Failed to review PR #%d in %s", pullRequest.getNumber(), repoName);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
