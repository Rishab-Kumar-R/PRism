package dev.rishabkumar.prism.review;

import dev.rishabkumar.prism.ai.CodeReview;
import dev.rishabkumar.prism.ai.AIReviewService;
import dev.rishabkumar.prism.github.GitHubService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.Optional;

@ApplicationScoped
public class ReviewService {

    @ConfigProperty(name = "app.review.cooldown-seconds", defaultValue = "60")
    int cooldownSeconds;

    @Inject
    GitHubService gitHubService;

    @Inject
    AIReviewService aiReviewService;

    @Inject
    ReviewRepository reviewRepository;

    @Inject
    ReviewMetrics reviewMetrics;

    @Inject
    PausedPrRepository pausedPrRepository;

    @Transactional
    public void review(GHPullRequest pullRequest, GHRepository repository) throws IOException {
        review(pullRequest, repository, false);
    }

    @Transactional
    public void reviewManual(GHPullRequest pullRequest, GHRepository repository) throws IOException {
        review(pullRequest, repository, true);
    }

    @Transactional
    void review(GHPullRequest pullRequest, GHRepository repository, boolean manual) throws IOException {
        String repoName = gitHubService.getRepoName(repository);
        String commitSha = pullRequest.getHead().getSha();
        int prNumber = pullRequest.getNumber();

        Log.infof("[%s#%d] Review requested for commit %s", repoName, prNumber, commitSha);

        if (reviewRepository.existsByCommitSha(repoName, prNumber, commitSha)) {
            Log.infof("[%s#%d] Skipping - commit %s already reviewed", repoName, prNumber, commitSha);
            reviewMetrics.recordSkipped("duplicate-commit");
            return;
        }

        if (reviewRepository.wasRecentlyReviewed(repoName, prNumber, cooldownSeconds)) {
            Log.infof("[%s#%d] Skipping - reviewed within last %ds", repoName, prNumber, cooldownSeconds);
            reviewMetrics.recordSkipped("cooldown");
            return;
        }

        if (!manual && pausedPrRepository.isPaused(repoName, prNumber)) {
            Log.infof("[%s#%d] Skipping - auto-review is paused", repoName, prNumber);
            reviewMetrics.recordSkipped("paused");
            return;
        }

        Optional<ReviewRecord> previousReview = reviewRepository.findLatestByPr(repoName, prNumber);
        String previousContext = previousReview.map(r -> """
                Score: %d/10, Severity: %s
                Bugs: %d, Security issues: %d, Performance issues: %d
                Recommendation: %s
                """.formatted(
                r.getScore(), r.getSeverity(),
                r.getBugCount(), r.getSecurityCount(), r.getPerformanceCount(),
                r.getRecommendation()
        )).orElse(null);

        if (previousContext != null) {
            Log.infof("[%s#%d] Found previous review for context", repoName, prNumber);
        }

        long startMs = System.currentTimeMillis();

        try {
            Log.infof("[%s#%d] Fetching diff", repoName, prNumber);
            String diff = gitHubService.fetchDiff(pullRequest);

            Log.infof("[%s#%d] Sending %d chars to AI", repoName, prNumber, diff.length());
            CodeReview codeReview = aiReviewService.review(diff, previousContext);

            if (codeReview == null) {
                Log.warnf("[%s#%d] AI returned null review - posting fallback comment", repoName, prNumber);
                gitHubService.postReviewComment(pullRequest, "No diff available to review.");
                reviewMetrics.recordError(repoName, System.currentTimeMillis() - startMs);
                return;
            }

            gitHubService.postReviewComment(pullRequest, codeReview.getFullReview());

            boolean wasLargePr = diff.contains("[Diff truncated");
            gitHubService.applyLabel(pullRequest, codeReview.getSeverity(), wasLargePr);

            ReviewRecord record = new ReviewRecord(
                    repoName, prNumber, pullRequest.getTitle(), commitSha,
                    codeReview.getSeverity(), codeReview.getScore(),
                    codeReview.getBugCount(), codeReview.getSecurityCount(),
                    codeReview.getPerformanceCount(), codeReview.getCodeQualityCount(),
                    codeReview.getRecommendation(), codeReview.getFullReview()
            );
            reviewRepository.persist(record);

            long duration = System.currentTimeMillis() - startMs;
            Log.infof("[%s#%d] Review complete - score: %d, severity: %s, duration: %dms",
                    repoName, prNumber, codeReview.getScore(), codeReview.getSeverity(), duration);
            reviewMetrics.recordSuccess(repoName, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs;
            Log.errorf(e, "[%s#%d] Review failed after %dms - posting fallback comment", repoName, prNumber, duration);
            reviewMetrics.recordError(repoName, duration);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }
}
