package dev.rishabkumar.prism.github.handler;

import dev.rishabkumar.prism.ai.service.WalkthroughService;
import dev.rishabkumar.prism.review.service.ReviewService;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kohsuke.github.GHEventPayload;

public class PullRequestHandler {

    @Inject
    ReviewService reviewService;

    @Inject
    WalkthroughService walkthroughService;

    @Inject
    ManagedExecutor executor;

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest payload) {
        var pullRequest = payload.getPullRequest();
        var repository = payload.getRepository();
        long installationId = extractInstallationId(payload);
        String accountName = extractAccountName(payload);

        executor.submit(() -> {
            try {
                walkthroughService.walkthrough(pullRequest, repository, installationId, accountName);
            } catch (Exception e) {
                Log.errorf(e, "Async walkthrough failed for PR #%d", pullRequest.getNumber());
            }
        });

        executor.submit(() -> {
            try {
                reviewService.review(pullRequest, repository, installationId, accountName);
            } catch (Exception e) {
                Log.errorf(e, "Async review failed for PR #%d", pullRequest.getNumber());
            }
        });
    }

    void onPullRequestReadyForReview(@PullRequest.ReadyForReview GHEventPayload.PullRequest payload) {
        var pullRequest = payload.getPullRequest();
        var repository = payload.getRepository();
        long installationId = extractInstallationId(payload);
        String accountName = extractAccountName(payload);

        executor.submit(() -> {
            try {
                walkthroughService.walkthrough(pullRequest, repository, installationId, accountName);
            } catch (Exception e) {
                Log.errorf(e, "Async walkthrough failed for PR #%d", pullRequest.getNumber());
            }
        });

        executor.submit(() -> {
            try {
                reviewService.review(pullRequest, repository, installationId, accountName);
            } catch (Exception e) {
                Log.errorf(e, "Async review failed for PR #%d", pullRequest.getNumber());
            }
        });
    }

    void onPullRequestSynchronized(@PullRequest.Synchronize GHEventPayload.PullRequest payload) {
        var pullRequest = payload.getPullRequest();
        var repository = payload.getRepository();
        long installationId = extractInstallationId(payload);
        String accountName = extractAccountName(payload);

        executor.submit(() -> {
            try {
                reviewService.review(pullRequest, repository, installationId, accountName);
            } catch (Exception e) {
                Log.errorf(e, "Async review failed for PR #%d", pullRequest.getNumber());
            }
        });
    }

    private long extractInstallationId(GHEventPayload.PullRequest payload) {
        return payload.getInstallation() != null ? payload.getInstallation().getId() : 0L;
    }

    private String extractAccountName(GHEventPayload.PullRequest payload) {
        if (payload.getInstallation() == null) return "unknown";
        if (payload.getInstallation().getAccount() == null) return "unknown";
        return payload.getInstallation().getAccount().getLogin();
    }
}
