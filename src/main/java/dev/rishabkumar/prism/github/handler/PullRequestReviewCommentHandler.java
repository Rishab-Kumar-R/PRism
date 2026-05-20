package dev.rishabkumar.prism.github.handler;

import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.review.service.ConversationService;
import io.quarkiverse.githubapp.event.PullRequestReviewComment;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class PullRequestReviewCommentHandler {

    @Inject
    ConversationService conversationService;

    @Inject
    GitHubService gitHubService;

    @Inject
    ManagedExecutor executor;

    void onReviewComment(@PullRequestReviewComment.Created GHEventPayload.PullRequestReviewComment payload) throws IOException {
        GHPullRequestReviewComment comment = payload.getComment();
        String body = comment.getBody();
        if (body == null) {
            return;
        }

        String trimmed = body.trim();
        if (!trimmed.toLowerCase().startsWith("@prism ")) {
            return;
        }

        String question = trimmed.substring("@prism ".length()).trim();
        if (question.isBlank()) {
            comment.reply("Hi! Ask me anything about this PR. Usage: `@prism <your question>`");
            return;
        }

        GHPullRequest pullRequest = payload.getPullRequest();
        GHRepository repository = payload.getRepository();
        int prNumber = pullRequest.getNumber();
        long installationId = payload.getInstallation() != null ? payload.getInstallation().getId() : 0L;
        String accountName = payload.getInstallation() != null && payload.getInstallation().getAccount() != null
                ? payload.getInstallation().getAccount().getLogin() : "unknown";

        String threadContext = buildThreadContext(comment, pullRequest);

        executor.submit(() -> {
            try {
                conversationService.answerInThread(pullRequest, repository, question, comment, threadContext, installationId, accountName);
            } catch (Exception e) {
                Log.errorf(e, "[PR #%d] Async @prism inline reply failed", prNumber);
                try {
                    comment.reply("Sorry, I could not answer that right now. Please try again.");
                } catch (IOException ex) {
                    Log.errorf(ex, "[PR #%d] Failed to post inline conversation error reply", prNumber);
                }
            }
        });
    }

    private String buildThreadContext(GHPullRequestReviewComment triggerComment, GHPullRequest pullRequest) {
        long inReplyToId = triggerComment.getInReplyToId();
        if (inReplyToId <= 0) {
            return null;
        }

        try {
            List<GHPullRequestReviewComment> threadComments = pullRequest.listReviewComments().toList()
                    .stream()
                    .filter(c -> c.getId() == inReplyToId || c.getInReplyToId() == inReplyToId)
                    .sorted(Comparator.comparingLong(GHPullRequestReviewComment::getId))
                    .toList();

            if (threadComments.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (GHPullRequestReviewComment c : threadComments) {
                if (c.getId() == triggerComment.getId()) {
                    continue;
                }
                String author = c.getUser() != null ? c.getUser().getLogin() : "unknown";
                sb.append(author).append(": ").append(c.getBody()).append("\n\n");
            }
            return sb.isEmpty() ? null : sb.toString().trim();
        } catch (Exception e) {
            Log.warnf("Could not fetch thread context for comment %d on PR #%d: %s",
                    triggerComment.getId(), pullRequest.getNumber(), e.getMessage());
            return null;
        }
    }
}
