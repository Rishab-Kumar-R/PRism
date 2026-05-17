package dev.rishabkumar.review;

import dev.rishabkumar.ai.CodeReview;
import dev.rishabkumar.ai.GeminiReviewService;
import dev.rishabkumar.github.GitHubService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

@ApplicationScoped
public class ReviewService {

    @ConfigProperty(name = "app.review.cooldown-seconds", defaultValue = "60")
    int cooldownSeconds;

    @Inject
    GitHubService gitHubService;

    @Inject
    GeminiReviewService geminiReviewService;

    @Inject
    ReviewRepository reviewRepository;

    @Transactional
    public void review(GHPullRequest pullRequest, GHRepository repository) throws IOException {
        String repoName = gitHubService.getRepoName(repository);
        String commitSha = pullRequest.getHead().getSha();

        Log.infof("Received review request for PR #%d in %s at commit %s", pullRequest.getNumber(), repoName, commitSha);

        if (reviewRepository.existsByCommitSha(repoName, pullRequest.getNumber(), commitSha)) {
            Log.infof("Review already exists for commit %s on PR #%d, skipping", commitSha, pullRequest.getNumber());
            return;
        }

        if (reviewRepository.wasRecentlyReviewed(repoName, pullRequest.getNumber(), cooldownSeconds)) {
            Log.infof("Rate limit: PR #%d in %s was reviewed in the last 60 seconds, skipping", pullRequest.getNumber(), repoName);
            return;
        }

        try {
            String diff = gitHubService.fetchDiff(pullRequest);
            CodeReview codeReview = geminiReviewService.review(diff);

            if (codeReview == null) {
                gitHubService.postReviewComment(pullRequest, "No diff available to review.");
                return;
            }

            gitHubService.postReviewComment(pullRequest, codeReview.getFullReview());

            boolean wasLargePr = diff.contains("[Diff truncated");
            gitHubService.applyLabel(pullRequest, codeReview.getSeverity(), wasLargePr);

            ReviewRecord record = new ReviewRecord(
                    repoName,
                    pullRequest.getNumber(),
                    pullRequest.getTitle(),
                    commitSha,
                    codeReview.getSeverity(),
                    codeReview.getScore(),
                    codeReview.getBugCount(),
                    codeReview.getSecurityCount(),
                    codeReview.getPerformanceCount(),
                    codeReview.getCodeQualityCount(),
                    codeReview.getRecommendation(),
                    codeReview.getFullReview()
            );
            reviewRepository.persist(record);

            Log.infof("Review completed - score: %d, severity: %s, commit: %s", codeReview.getScore(), codeReview.getSeverity(), commitSha);
        } catch (Exception e) {
            Log.errorf(e, "Failed to review PR #%d in %s", pullRequest.getNumber(), repoName);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
