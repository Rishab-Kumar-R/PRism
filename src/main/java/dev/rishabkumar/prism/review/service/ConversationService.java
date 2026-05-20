package dev.rishabkumar.prism.review.service;

import dev.rishabkumar.prism.ai.client.ConversationAI;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.service.RateLimitService;
import dev.rishabkumar.prism.review.model.PreviousReviewContext;
import dev.rishabkumar.prism.review.repository.ReviewRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class ConversationService {

    @ConfigProperty(name = "app.review.max-diff-chars", defaultValue = "50000")
    int maxDiffChars;

    @Inject
    ConversationAI conversationAI;

    @Inject
    GitHubService gitHubService;

    @Inject
    ReviewRepository reviewRepository;

    @Inject
    RateLimitService rateLimitService;

    public void answer(GHPullRequest pullRequest, GHRepository repository,
                       String question, long installationId, String accountName) throws IOException {
        String answer = generateAnswer(pullRequest, repository, question, null, installationId, accountName);
        if (answer != null) {
            gitHubService.postReviewComment(pullRequest, answer);
        }
    }

    public void answerInThread(GHPullRequest pullRequest, GHRepository repository,
                               String question, GHPullRequestReviewComment triggerComment,
                               String threadContext, long installationId, String accountName) throws IOException {
        String answer = generateAnswer(pullRequest, repository, question, threadContext, installationId, accountName);
        if (answer != null) {
            triggerComment.reply(answer);
        }
    }

    private String generateAnswer(GHPullRequest pullRequest, GHRepository repository,
                                   String question, String threadContext,
                                   long installationId, String accountName) throws IOException {
        String repoName = gitHubService.getRepoName(repository);
        int prNumber = pullRequest.getNumber();

        RateLimitResult limitResult = rateLimitService.check(installationId, accountName);
        if (limitResult.isExceeded()) {
            gitHubService.postReviewComment(pullRequest,
                    "Review budget exhausted for this installation (%s plan). Questions will be available again when the limit resets."
                            .formatted(limitResult.tier()));
            return null;
        }

        Optional<PreviousReviewContext> previousReview = reviewRepository.findLatestContextByPr(
                repoName, prNumber, LocalDateTime.MIN);
        String diff = gitHubService.fetchDiff(pullRequest);

        String prompt = buildPrompt(question, diff, previousReview.orElse(null), threadContext);
        String answer = conversationAI.answer(prompt);

        rateLimitService.record(installationId);
        Log.infof("[%s#%d] Answered question: %s", repoName, prNumber, question);

        return answer;
    }

    private String buildPrompt(String question, String diff,
                                PreviousReviewContext previousReview, String threadContext) {
        StringBuilder sb = new StringBuilder();

        if (previousReview != null) {
            sb.append("Previous review summary: Score %d/10 (%s). Bugs: %d, Security: %d, Performance: %d. Recommendation: %s\n\n"
                    .formatted(
                            previousReview.score(), previousReview.severity(),
                            previousReview.bugCount(), previousReview.securityCount(),
                            previousReview.performanceCount(), previousReview.recommendation()));
        }

        if (threadContext != null) {
            sb.append("Thread context:\n").append(threadContext).append("\n\n");
        }

        String truncatedDiff = diff.length() > maxDiffChars
                ? diff.substring(0, maxDiffChars) + "\n[Diff truncated]"
                : diff;

        sb.append("PR diff:\n").append(truncatedDiff).append("\n\n");
        sb.append("Question: ").append(question);

        return sb.toString();
    }
}
