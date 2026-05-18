package dev.rishabkumar.prism.review.service;

import dev.rishabkumar.prism.ai.model.CodeReview;
import dev.rishabkumar.prism.ai.model.ReviewOutcome;
import dev.rishabkumar.prism.ai.service.AIReviewService;
import dev.rishabkumar.prism.exception.PrAlreadyPausedException;
import dev.rishabkumar.prism.exception.PrNotPausedException;
import dev.rishabkumar.prism.exception.ReviewNotFoundException;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.model.RateLimitStatus;
import dev.rishabkumar.prism.ratelimit.service.RateLimitService;
import dev.rishabkumar.prism.review.model.PausedPr;
import dev.rishabkumar.prism.review.model.PreviousReviewContext;
import dev.rishabkumar.prism.review.model.ReviewRecord;
import dev.rishabkumar.prism.review.model.ReviewStats;
import dev.rishabkumar.prism.review.model.ReviewSummary;
import dev.rishabkumar.prism.review.repository.PausedPrRepository;
import dev.rishabkumar.prism.review.repository.ReviewRepository;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.List;
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

    @Inject
    RateLimitService rateLimitService;

    @Transactional
    public void pause(GHPullRequest pullRequest, GHRepository repository) throws IOException {
        String repoName = gitHubService.getRepoName(repository);
        int prNumber = pullRequest.getNumber();

        if (pausedPrRepository.isPaused(repoName, prNumber)) {
            throw new PrAlreadyPausedException(
                    "Auto-review is already paused for PR #" + prNumber + " in " + repoName);
        }

        pausedPrRepository.persist(new PausedPr(repoName, prNumber));
        Log.infof("[%s#%d] Auto-review paused", repoName, prNumber);
        gitHubService.postReviewComment(pullRequest,
                "Auto-review paused for this PR. PRism will no longer review new commits automatically.\n\nUse `/review resume` to re-enable, or `/review` to trigger a manual review.");
    }

    @Transactional
    public void resume(GHPullRequest pullRequest, GHRepository repository) throws IOException {
        String repoName = gitHubService.getRepoName(repository);
        int prNumber = pullRequest.getNumber();

        if (!pausedPrRepository.isPaused(repoName, prNumber)) {
            throw new PrNotPausedException(
                    "Auto-review is not paused for PR #" + prNumber + " in " + repoName);
        }

        pausedPrRepository.resume(repoName, prNumber);
        Log.infof("[%s#%d] Auto-review resumed", repoName, prNumber);
        gitHubService.postReviewComment(pullRequest,
                "Auto-review resumed. PRism will review new commits automatically again.");
    }

    public List<ReviewSummary> getAll(int page, int size) {
        return reviewRepository.findAllPaged(page, size).stream().map(ReviewSummary::new).toList();
    }

    public ReviewRecord getById(Long id) {
        ReviewRecord record = reviewRepository.findById(id);
        if (record == null) {
            throw new ReviewNotFoundException("Review not found: " + id);
        }
        return record;
    }

    public List<ReviewSummary> getByRepo(String repoName, int page, int size) {
        return reviewRepository.findByRepo(repoName, page, size).stream().map(ReviewSummary::new).toList();
    }

    public List<ReviewSummary> getByPr(int prNumber, int page, int size) {
        return reviewRepository.findByPr(prNumber, page, size).stream().map(ReviewSummary::new).toList();
    }

    public List<ReviewSummary> getByRepoAndPr(String repoName, int prNumber, int page, int size) {
        return reviewRepository.findByRepoAndPr(repoName, prNumber, page, size).stream().map(ReviewSummary::new).toList();
    }

    @CacheResult(cacheName = "review-stats")
    public ReviewStats getStats() {
        Object[] agg = reviewRepository.aggregateStats();
        long total = agg[0] != null ? ((Number) agg[0]).longValue() : 0L;
        long approved = agg[1] != null ? ((Number) agg[1]).longValue() : 0L;
        long needsWork = agg[2] != null ? ((Number) agg[2]).longValue() : 0L;
        double avgScore = agg[3] != null ? ((Number) agg[3]).doubleValue() : 0.0;
        long totalBugs = agg[4] != null ? ((Number) agg[4]).longValue() : 0L;
        long totalSecurity = agg[5] != null ? ((Number) agg[5]).longValue() : 0L;
        long totalPerformance = agg[6] != null ? ((Number) agg[6]).longValue() : 0L;
        String mostReviewedRepo = reviewRepository.mostReviewedRepo();
        return new ReviewStats(total, approved, needsWork, avgScore,
                totalBugs, totalSecurity, totalPerformance, mostReviewedRepo);
    }

    @CacheInvalidate(cacheName = "review-stats")
    public void invalidateStatsCache() {
    }

    @Transactional
    public void review(GHPullRequest pullRequest, GHRepository repository, long installationId, String accountName) throws IOException {
        review(pullRequest, repository, installationId, accountName, false);
    }

    @Transactional
    public void reviewManual(GHPullRequest pullRequest, GHRepository repository, long installationId, String accountName) throws IOException {
        review(pullRequest, repository, installationId, accountName, true);
    }

    @Transactional
    void review(GHPullRequest pullRequest, GHRepository repository, long installationId, String accountName, boolean manual) throws IOException {
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

        Optional<PreviousReviewContext> previousReview = reviewRepository.findLatestContextByPr(repoName, prNumber);
        String baseSha = previousReview.map(PreviousReviewContext::commitSha).orElse(null);
        String previousContext = previousReview.map(r -> """
                Score: %d/10, Severity: %s
                Bugs: %d, Security issues: %d, Performance issues: %d
                Recommendation: %s
                """.formatted(
                r.score(), r.severity(),
                r.bugCount(), r.securityCount(), r.performanceCount(),
                r.recommendation()
        )).orElse(null);

        if (previousContext != null) {
            Log.infof("[%s#%d] Found previous review at %s, fetching incremental diff", repoName, prNumber, baseSha);
        }

        RateLimitResult limitResult = rateLimitService.check(installationId, accountName);
        if (limitResult.isExceeded()) {
            Log.warnf("[%s#%d] Review budget exhausted for installation %d (%d/%d monthly, %d/%d daily)",
                    repoName, prNumber, installationId,
                    limitResult.reviewsUsed(), limitResult.reviewBudget(),
                    limitResult.dailyReviewsUsed(), limitResult.dailyReviewBudget());
            gitHubService.postReviewComment(pullRequest, buildRateLimitExceededComment(limitResult));
            reviewMetrics.recordSkipped("rate-limit-exceeded");
            return;
        }

        long startMs = System.currentTimeMillis();

        try {
            String diff = gitHubService.fetchDiff(pullRequest, baseSha);

            Log.infof("[%s#%d] Sending %d chars to AI", repoName, prNumber, diff.length());
            ReviewOutcome outcome = aiReviewService.review(diff, previousContext);

            if (outcome == null) {
                Log.warnf("[%s#%d] AI returned null review - posting fallback comment", repoName, prNumber);
                gitHubService.postReviewComment(pullRequest, "No diff available to review.");
                reviewMetrics.recordError(repoName, System.currentTimeMillis() - startMs);
                return;
            }

            CodeReview codeReview = outcome.review();
            rateLimitService.record(installationId);

            String reviewComment = buildReviewComment(codeReview.fullReview(), limitResult);
            gitHubService.postReviewComment(pullRequest, reviewComment);

            gitHubService.applyLabel(pullRequest, codeReview.severity(), outcome.chunked());

            ReviewRecord record = new ReviewRecord(
                    repoName, prNumber, pullRequest.getTitle(), commitSha,
                    codeReview.severity(), codeReview.score(),
                    codeReview.bugCount(), codeReview.securityCount(),
                    codeReview.performanceCount(), codeReview.codeQualityCount(),
                    codeReview.recommendation(), codeReview.fullReview()
            );
            reviewRepository.persist(record);
            invalidateStatsCache();

            long duration = System.currentTimeMillis() - startMs;
            Log.infof("[%s#%d] Review complete - score: %d, severity: %s, duration: %dms",
                    repoName, prNumber, codeReview.score(), codeReview.severity(), duration);
            reviewMetrics.recordSuccess(repoName, duration);
        } catch (jakarta.persistence.PersistenceException e) {
            if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                Log.infof("[%s#%d] Duplicate webhook delivery detected for commit %s - skipping", repoName, prNumber, commitSha);
                reviewMetrics.recordSkipped("duplicate-webhook");
            } else {
                long duration = System.currentTimeMillis() - startMs;
                Log.errorf(e, "[%s#%d] Review failed after %dms - posting fallback comment", repoName, prNumber, duration);
                reviewMetrics.recordError(repoName, duration);
                gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startMs;
            Log.errorf(e, "[%s#%d] Review failed after %dms - posting fallback comment", repoName, prNumber, duration);
            reviewMetrics.recordError(repoName, duration);
            gitHubService.postReviewComment(pullRequest, "AI review is temporarily unavailable. Please try again later.");
        }
    }

    private String buildReviewComment(String fullReview, RateLimitResult limit) {
        if (limit.status() != RateLimitStatus.WARNING) {
            return fullReview;
        }
        return fullReview + """


                ---
                > **Usage warning:** This repository has used %d%% of its monthly review budget (%s tier: %d reviews/month). \
                Reviews will be blocked when the budget is exhausted."""
                .formatted(limit.percentUsed(), limit.tier(), limit.reviewBudget());
    }

    private String buildRateLimitExceededComment(RateLimitResult limit) {
        if (limit.isDailyExceeded()) {
            return """
                    ## PRism - Daily Limit Reached

                    This repository has used all %d reviews allowed today (%s plan).
                    Reviews will resume automatically tomorrow.

                    To increase your daily limit, upgrade your PRism plan.
                    """.formatted(limit.dailyReviewBudget(), limit.tier());
        }
        return """
                ## PRism - Monthly Budget Exhausted

                This repository has used all %d reviews included in the %s plan this month.
                Automatic reviews are paused until the 1st of next month.

                To increase your limit, upgrade your PRism plan.
                """.formatted(limit.reviewBudget(), limit.tier());
    }
}
