package dev.rishabkumar.github;

import dev.rishabkumar.review.ReviewService;
import io.quarkiverse.githubapp.event.PullRequest;
import jakarta.inject.Inject;
import org.kohsuke.github.GHEventPayload;

import java.io.IOException;

public class PullRequestHandler {

    @Inject
    ReviewService reviewService;

    void onPullRequestOpened(@PullRequest.Opened GHEventPayload.PullRequest payload) throws IOException {
        reviewService.review(payload.getPullRequest(), payload.getRepository());
    }

    void onPullRequestSynchronized(@PullRequest.Synchronize GHEventPayload.PullRequest payload) throws IOException {
        reviewService.review(payload.getPullRequest(), payload.getRepository());
    }
}
