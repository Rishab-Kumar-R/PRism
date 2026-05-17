package dev.rishabkumar.github;

import dev.rishabkumar.ai.CodeReview;
import dev.rishabkumar.ai.GeminiReviewService;
import dev.rishabkumar.review.ReviewRecord;
import dev.rishabkumar.review.ReviewRepository;
import dev.rishabkumar.review.ReviewService;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

public class IssueCommentHandler {

    @Inject
    ReviewService reviewService;

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
        reviewService.review(pullRequest, payload.getRepository());
    }
}
