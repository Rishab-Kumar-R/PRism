package dev.rishabkumar.prism.github;

import dev.rishabkumar.prism.ai.SummaryService;
import dev.rishabkumar.prism.review.ReviewService;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

public class IssueCommentHandler {

    @Inject
    ReviewService reviewService;

    @Inject
    SummaryService summaryService;

    @Inject
    ManagedExecutor executor;

    void onIssueComment(@IssueComment.Created GHEventPayload.IssueComment payload) throws IOException {
        String body = payload.getComment().getBody();
        if (body == null) {
            return;
        }

        String command = body.trim().toLowerCase();

        if (!command.equals("/review") && !command.equals("/review pause")
                && !command.equals("/review resume") && !command.equals("/summary")) {
            return;
        }

        if (!payload.getIssue().isPullRequest()) {
            return;
        }

        GHPullRequest pullRequest = payload.getRepository().getPullRequest(payload.getIssue().getNumber());
        GHRepository repository = payload.getRepository();
        int prNumber = pullRequest.getNumber();

        switch (command) {
            case "/review" -> executor.submit(() -> {
                try {
                    reviewService.reviewManual(pullRequest, repository);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review failed", prNumber);
                }
            });
            case "/review pause" -> executor.submit(() -> {
                try {
                    reviewService.pause(pullRequest, repository);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review pause failed", prNumber);
                }
            });
            case "/review resume" -> executor.submit(() -> {
                try {
                    reviewService.resume(pullRequest, repository);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review resume failed", prNumber);
                }
            });
            case "/summary" -> executor.submit(() -> {
                try {
                    summaryService.summarize(pullRequest, repository);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /summary failed", prNumber);
                }
            });
        }
    }
}
