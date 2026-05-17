package dev.rishabkumar.prism.github;

import dev.rishabkumar.prism.review.ReviewService;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kohsuke.github.GHEventPayload;

import java.io.IOException;

public class IssueCommentHandler {

    @Inject
    ReviewService reviewService;

    @Inject
    ManagedExecutor executor;

    void onIssueComment(@IssueComment.Created GHEventPayload.IssueComment payload) throws IOException {
        String body = payload.getComment().getBody();
        if (body == null || !body.trim().equalsIgnoreCase("/review")) {
            return;
        }

        if (!payload.getIssue().isPullRequest()) {
            return;
        }

        var pullRequest = payload.getRepository().getPullRequest(payload.getIssue().getNumber());
        var repository = payload.getRepository();

        executor.submit(() -> {
            try {
                reviewService.review(pullRequest, repository);
            } catch (Exception e) {
                Log.errorf(e, "Async /review failed for PR #%d", pullRequest.getNumber());
            }
        });
    }
}
