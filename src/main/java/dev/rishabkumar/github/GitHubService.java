package dev.rishabkumar.github;

import jakarta.enterprise.context.ApplicationScoped;
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

    private static final int MAX_DIFF_CHARS = 50_000;

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

        if (diff.length() > MAX_DIFF_CHARS) {
            return diff.substring(0, MAX_DIFF_CHARS) + "\n\n[Diff truncated - too large for review]";
        }

        return diff;
    }

    public void postReviewComment(GHPullRequest pullRequest, String review) throws IOException {
        pullRequest.comment(review);
    }

    public String getRepoName(GHRepository repository) {
        return repository.getFullName();
    }
}
