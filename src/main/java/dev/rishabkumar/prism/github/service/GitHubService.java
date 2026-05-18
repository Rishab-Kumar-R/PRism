package dev.rishabkumar.prism.github.service;

import dev.rishabkumar.prism.exception.DiffFetchException;
import io.quarkus.logging.Log;
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

    public void postReviewComment(GHPullRequest pullRequest, String review) throws IOException {
        pullRequest.comment(review);
        Log.infof("Posted review comment on PR #%d", pullRequest.getNumber());
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
}
