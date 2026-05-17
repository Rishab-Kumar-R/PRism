package dev.rishabkumar.prism.github;

import dev.rishabkumar.prism.review.ReviewService;
import io.quarkiverse.githubapp.event.PullRequest;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kohsuke.github.GHEventPayload;

public class PullRequestHandler {

    @Inject
    ReviewService reviewService;

    @Inject
    ManagedExecutor executor;

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest payload) {
        var pullRequest = payload.getPullRequest();
        var repository = payload.getRepository();

        executor.submit(() -> {
            try {
                reviewService.review(pullRequest, repository);
            } catch (Exception e) {
                Log.errorf(e, "Async review failed for PR #%d", pullRequest.getNumber());
            }
        });
    }

    void onPullRequestSynchronized(@PullRequest.Synchronize GHEventPayload.PullRequest payload) {
        var pullRequest = payload.getPullRequest();
        var repository = payload.getRepository();

        executor.submit(() -> {
            try {
                reviewService.review(pullRequest, repository);
            } catch (Exception e) {
                Log.errorf(e, "Async review failed for PR #%d", pullRequest.getNumber());
            }
        });
    }
}
