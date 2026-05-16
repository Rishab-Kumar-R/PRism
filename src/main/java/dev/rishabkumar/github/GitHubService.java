package dev.rishabkumar.github;

import jakarta.enterprise.context.ApplicationScoped;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.stream.Collectors;

@ApplicationScoped
public class GitHubService {

    public String fetchDiff(GHPullRequest pullRequest) throws IOException {
        String diffUrl = pullRequest.getDiffUrl().toString();

        HttpURLConnection connection = (HttpURLConnection) URI.create(diffUrl)
                .toURL()
                .openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github.v3.diff");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } finally {
            connection.disconnect();
        }
    }

    public void postReviewComment(GHPullRequest pullRequest, String review) throws IOException {
        pullRequest.comment(review);
    }

    public String getRepoName(GHRepository repository) {
        return repository.getFullName();
    }
}
