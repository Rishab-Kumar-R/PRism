package dev.rishabkumar.prism.github.service;

import dev.rishabkumar.prism.ai.model.InlineComment;
import dev.rishabkumar.prism.exception.DiffFetchException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.kohsuke.github.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GitHubService {

    public String fetchDiff(GHPullRequest pullRequest) {
        return fetchDiff(pullRequest, null);
    }

    public String fetchDiff(GHPullRequest pullRequest, String baseSha) {
        try {
            return doFetchDiff(pullRequest, baseSha);
        } catch (IOException e) {
            throw new DiffFetchException("Failed to fetch diff for PR #" + pullRequest.getNumber(), e);
        }
    }

    private String doFetchDiff(GHPullRequest pullRequest, String baseSha) throws IOException {
        String diffUrl;

        if (baseSha != null) {
            String headSha = pullRequest.getHead().getSha();
            String repoUrl = pullRequest.getRepository().getHtmlUrl().toString();
            diffUrl = "%s/compare/%s...%s.diff".formatted(repoUrl, baseSha, headSha);
            Log.infof("Fetching incremental diff for PR #%d: %s...%s", pullRequest.getNumber(), baseSha.substring(0, 7), headSha.substring(0, 7));
        } else {
            diffUrl = pullRequest.getDiffUrl().toString();
            Log.infof("Fetching full diff for PR #%d", pullRequest.getNumber());
        }

        HttpURLConnection connection = (HttpURLConnection) URI.create(diffUrl)
                .toURL()
                .openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github.v3.diff");

        String diff;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            diff = reader.lines().collect(Collectors.joining("\n"));
        } finally {
            connection.disconnect();
        }

        Log.infof("Fetched diff of %d chars for PR #%d", diff.length(), pullRequest.getNumber());
        return diff;
    }

    public GHIssueComment postReviewComment(GHPullRequest pullRequest, String review) throws IOException {
        GHIssueComment comment = pullRequest.comment(review);
        Log.infof("Posted review comment on PR #%d", pullRequest.getNumber());
        return comment;
    }

    public void markCommentSuperseded(GHPullRequest pullRequest, long commentId) {
        try {
            pullRequest.listComments().toList().stream()
                    .filter(c -> c.getId() == commentId)
                    .findFirst()
                    .ifPresent(c -> {
                        try {
                            c.update("> This review has been superseded by a newer commit. See the latest review for updated feedback.");
                            Log.infof("Marked comment %d as superseded on PR #%d", commentId, pullRequest.getNumber());
                        } catch (IOException e) {
                            Log.warnf("Could not update comment %d: %s", commentId, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.warnf("Could not mark comment %d as superseded on PR #%d: %s", commentId, pullRequest.getNumber(), e.getMessage());
        }
    }

    public void applyLabel(GHPullRequest pullRequest, String severity, boolean wasLargePr) throws IOException {
        GHRepository repository = pullRequest.getRepository();

        String labelName;
        String labelColor;

        if (wasLargePr) {
            labelName = "ai: large-pr";
            labelColor = "FFA500";
        } else if ("APPROVED".equals(severity)) {
            labelName = "ai: approved";
            labelColor = "0075ca";
        } else {
            labelName = "ai: needs-work";
            labelColor = "e11d48";
        }

        try {
            repository.createLabel(labelName, labelColor);
        } catch (Exception e) {
            // label already exists, safe to ignore
        }

        pullRequest.addLabels(labelName);
        Log.infof("Applied label '%s' to PR #%d", labelName, pullRequest.getNumber());
    }

    public String getRepoName(GHRepository repository) {
        return repository.getFullName();
    }

    public GHPullRequestReview postInlineReview(GHPullRequest pullRequest,
                                                String commitSha,
                                                String summary,
                                                List<InlineComment> inlineComments,
                                                String diff) throws IOException {
        DiffPositionMapper mapper = DiffPositionMapper.parse(diff);

        var builder = pullRequest.createReview()
                .commitId(commitSha)
                .body(summary)
                .event(GHPullRequestReviewEvent.COMMENT);

        int added = 0;
        for (InlineComment c : inlineComments) {
            int position = mapper.getDiffPosition(c.path(), c.line());
            if (position < 1) {
                Log.warnf("Could not map inline comment for %s:%d - skipping", c.path(), c.line());
                continue;
            }

            String commentBody = c.suggestion() != null && !c.suggestion().isBlank()
                    ? c.body() + "\n\n```suggestion\n" + c.suggestion() + "\n```"
                    : c.body();
            builder.comment(commentBody, c.path(), position);
            added++;
        }

        Log.infof("Posted PR review with %d inline comments on PR #%d", added, pullRequest.getNumber());
        return builder.create();
    }

    public void postCommitStatus(GHRepository repository, String commitSha,
                                  boolean approved, int score, String recommendation) {
        GHCommitState state = approved ? GHCommitState.SUCCESS : GHCommitState.FAILURE;
        String description = "Score: %d/10 — %s".formatted(score,
                recommendation != null && recommendation.length() > 100
                        ? recommendation.substring(0, 97) + "..."
                        : recommendation);
        try {
            repository.createCommitStatus(commitSha, state, null, description, "PRism / code-review");
            Log.infof("Posted commit status %s for %s", state, commitSha.substring(0, 7));
        } catch (IOException e) {
            Log.warnf("Could not post commit status for %s: %s", commitSha.substring(0, 7), e.getMessage());
        }
    }

    public void dismissPreviousReview(GHPullRequest pullRequest, long reviewId) {
        try {
            pullRequest.listReviews().toList().stream()
                    .filter(r -> r.getId() == reviewId)
                    .findFirst()
                    .ifPresent(r -> {
                        try {
                            r.dismiss("Superseded by a newer review");
                            Log.infof("Dismissed previous review %d on PR #%d", reviewId, pullRequest.getNumber());
                        } catch (IOException e) {
                            Log.warnf("Failed to dismiss review %d: %s", reviewId, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.warnf("Could not dismiss previous review %d on PR #%d: %s", reviewId, pullRequest.getNumber(), e.getMessage());
        }
    }
}
