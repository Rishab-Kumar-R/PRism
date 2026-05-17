package dev.rishabkumar.github;

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
            String review = geminiReviewService.review(diff);

            gitHubService.postReviewComment(pullRequest, review);

            boolean wasLargePr = diff.contains("[Diff truncated");
            String severity = geminiReviewService.assessSeverity(review);
            gitHubService.applyLabel(pullRequest, severity, wasLargePr);

            ReviewRecord record = new ReviewRecord(
                    repoName,
                    pullRequest.getNumber(),
                    pullRequest.getTitle(),
                    commitSha,
                    review
            );
            reviewRepository.persist(record);

            Log.infof("Manual review completed for PR #%d in %s", pullRequest.getNumber(), repoName);
        } catch (Exception e) {
            Log.errorf(e, "Failed manual review for PR #%d in %s", pullRequest.getNumber(), repoName);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
