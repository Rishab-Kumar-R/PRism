package dev.rishabkumar.github;

import dev.rishabkumar.ai.CodeReview;
import dev.rishabkumar.ai.GeminiReviewService;
import dev.rishabkumar.review.ReviewRecord;
import dev.rishabkumar.review.ReviewRepository;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

public class IssueCommentHandler {

    @Inject
    GitHubService gitHubService;

    @Inject
    GeminiReviewService geminiReviewService;

    @Inject
    ReviewRepository reviewRepository;

    void onIssueComment(@IssueComment.Created GHEventPayload.IssueComment payload) throws IOException {
        String body = payload.getComment().getBody().trim();

        if (!body.equalsIgnoreCase("/review")) {
            return;
        }

        if (!payload.getIssue().isPullRequest()) {
            Log.info("Received /review on an issue, not a PR - skipping");
            return;
        }

        GHPullRequest pullRequest = payload.getRepository().getPullRequest(payload.getIssue().getNumber());
        String repoName = gitHubService.getRepoName(payload.getRepository());
        String commitSha = pullRequest.getHead().getSha();

        Log.infof("Manual /review triggered for PR #%d in %s", pullRequest.getNumber(), repoName);

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

            Log.infof("Manual review completed - score: %d, severity: %s", review.getScore(), review.getSeverity());
        } catch (Exception e) {
            Log.errorf(e, "Failed manual review for PR #%d in %s", pullRequest.getNumber(), repoName);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
