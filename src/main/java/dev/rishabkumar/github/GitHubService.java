package dev.rishabkumar.github;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.stream.Collectors;

@ApplicationScoped
public class GitHubService {

    @ConfigProperty(name = "app.review.max-diff-chars", defaultValue = "50000")
    int maxDiffChars;

    public String fetchDiff(GHPullRequest pullRequest) throws IOException {
        String diffUrl = pullRequest.getDiffUrl().toString();

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

        if (diff.length() > maxDiffChars) {
            Log.warnf("Diff exceeds limit (%d chars), truncating to %d", diff.length(), maxDiffChars);
            return diff.substring(0, maxDiffChars) + "\n\n[Diff truncated - too large for review]";
        }

        return diff;
    }

    public void postReviewComment(GHPullRequest pullRequest, String review) throws IOException {
        pullRequest.comment(review);
        Log.infof("Posted review comment on PR #%d", pullRequest.getNumber());
    }

    public String getRepoName(GHRepository repository) {
        return repository.getFullName();
    }
}
