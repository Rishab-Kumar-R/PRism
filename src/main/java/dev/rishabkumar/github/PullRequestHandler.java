package dev.rishabkumar.github;

import dev.rishabkumar.ai.CodeReview;
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
            CodeReview review = geminiReviewService.review(diff);

            if (review == null) {
                gitHubService.postReviewComment(pullRequest, "No diff available to review.");
                return;
            }

            gitHubService.postReviewComment(pullRequest, review.getFullReview());

            boolean wasLargePr = diff.contains("[Diff truncated");
            gitHubService.applyLabel(pullRequest, review.getSeverity(), wasLargePr);

            ReviewRecord record = new ReviewRecord(
                    repoName,
                    pullRequest.getNumber(),
                    pullRequest.getTitle(),
                    commitSha,
                    review.getSeverity(),
                    review.getScore(),
                    review.getBugCount(),
                    review.getSecurityCount(),
                    review.getPerformanceCount(),
                    review.getCodeQualityCount(),
                    review.getRecommendation(),
                    review.getFullReview()
            );

            reviewRepository.persist(record);
            Log.infof("Review completed - score: %d, severity: %s, commit: %s", review.getScore(), review.getSeverity(), commitSha);
        } catch (Exception e) {
            Log.errorf(e, "Failed to review PR #%d in %s", pullRequest.getNumber(), repoName);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
