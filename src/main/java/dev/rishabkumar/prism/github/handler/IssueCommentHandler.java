package dev.rishabkumar.prism.github.handler;

import dev.rishabkumar.prism.ai.service.SummaryService;
import dev.rishabkumar.prism.exception.PrAlreadyPausedException;
import dev.rishabkumar.prism.exception.PrNotPausedException;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.review.service.ConversationService;
import dev.rishabkumar.prism.review.service.ReviewService;
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
    GitHubService gitHubService;

    @Inject
    ConversationService conversationService;

    @Inject
    ManagedExecutor executor;

    void onIssueComment(@IssueComment.Created GHEventPayload.IssueComment payload) throws IOException {
        String body = payload.getComment().getBody();
        if (body == null) {
            return;
        }

        String trimmedBody = body.trim();
        String command = trimmedBody.toLowerCase();

        boolean isAskCommand = command.startsWith("/review ask ");
        boolean isPrismMention = command.startsWith("@prism ");

        if (!command.equals("/review") && !command.equals("/review pause")
                && !command.equals("/review resume") && !command.equals("/review reset")
                && !command.equals("/summary") && !isAskCommand && !isPrismMention) {
            return;
        }

        if (!payload.getIssue().isPullRequest()) {
            return;
        }

        GHPullRequest pullRequest = payload.getRepository().getPullRequest(payload.getIssue().getNumber());
        GHRepository repository = payload.getRepository();
        int prNumber = pullRequest.getNumber();
        long installationId = payload.getInstallation() != null ? payload.getInstallation().getId() : 0L;
        String accountName = payload.getInstallation() != null && payload.getInstallation().getAccount() != null
                ? payload.getInstallation().getAccount().getLogin() : "unknown";

        if (isAskCommand) {
            String question = trimmedBody.substring("/review ask ".length()).trim();
            if (question.isBlank()) {
                gitHubService.postReviewComment(pullRequest,
                        "Please include a question. Usage: `/review ask <your question>`");
                return;
            }
            executor.submit(() -> {
                try {
                    conversationService.answer(pullRequest, repository, question, installationId, accountName);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review ask failed", prNumber);
                    try {
                        gitHubService.postReviewComment(pullRequest,
                                "Sorry, I could not answer that right now. Please try again.");
                    } catch (IOException ex) {
                        Log.errorf(ex, "[PR #%d] Failed to post conversation error comment", prNumber);
                    }
                }
            });
            return;
        }

        if (isPrismMention) {
            String question = trimmedBody.substring("@prism ".length()).trim();
            if (question.isBlank()) {
                gitHubService.postReviewComment(pullRequest,
                        "Hi! Ask me anything about this PR. Usage: `@prism <your question>`");
                return;
            }
            executor.submit(() -> {
                try {
                    conversationService.answer(pullRequest, repository, question, installationId, accountName);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async @prism mention failed", prNumber);
                    try {
                        gitHubService.postReviewComment(pullRequest,
                                "Sorry, I could not answer that right now. Please try again.");
                    } catch (IOException ex) {
                        Log.errorf(ex, "[PR #%d] Failed to post conversation error comment", prNumber);
                    }
                }
            });
            return;
        }

        switch (command) {
            case "/review" -> executor.submit(() -> {
                try {
                    reviewService.reviewManual(pullRequest, repository, installationId, accountName);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review failed", prNumber);
                }
            });
            case "/review pause" -> executor.submit(() -> {
                try {
                    reviewService.pause(pullRequest, repository);
                } catch (PrAlreadyPausedException e) {
                    try {
                        gitHubService.postReviewComment(pullRequest,
                                "Auto-review is already paused for this PR. Use `/review resume` to re-enable it.");
                    } catch (IOException ex) {
                        Log.errorf(ex, "[PR #%d] Failed to post already-paused comment", prNumber);
                    }
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review pause failed", prNumber);
                }
            });
            case "/review resume" -> executor.submit(() -> {
                try {
                    reviewService.resume(pullRequest, repository);
                } catch (PrNotPausedException e) {
                    try {
                        gitHubService.postReviewComment(pullRequest,
                                "Auto-review is not paused for this PR.");
                    } catch (IOException ex) {
                        Log.errorf(ex, "[PR #%d] Failed to post not-paused comment", prNumber);
                    }
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review resume failed", prNumber);
                }
            });
            case "/review reset" -> executor.submit(() -> {
                try {
                    reviewService.reset(pullRequest, repository);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /review reset failed", prNumber);
                }
            });
            case "/summary" -> executor.submit(() -> {
                try {
                    summaryService.summarize(pullRequest, repository, installationId, accountName);
                } catch (Exception e) {
                    Log.errorf(e, "[PR #%d] Async /summary failed", prNumber);
                }
            });
        }
    }
}
