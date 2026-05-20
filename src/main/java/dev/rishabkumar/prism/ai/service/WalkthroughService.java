package dev.rishabkumar.prism.ai.service;

import dev.rishabkumar.prism.ai.client.WalkthroughAI;
import dev.rishabkumar.prism.github.service.GitHubService;
import dev.rishabkumar.prism.ratelimit.model.RateLimitResult;
import dev.rishabkumar.prism.ratelimit.service.RateLimitService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

@ApplicationScoped
public class WalkthroughService {

    @Inject
    WalkthroughAI walkthroughAI;

    @Inject
    GitHubService gitHubService;

    @Inject
    RateLimitService rateLimitService;

    public void walkthrough(GHPullRequest pullRequest, GHRepository repository,
                            long installationId, String accountName) throws IOException {
        String repoName = gitHubService.getRepoName(repository);
        int prNumber = pullRequest.getNumber();

        Log.infof("[%s#%d] Walkthrough requested", repoName, prNumber);

        RateLimitResult limitResult = rateLimitService.check(installationId, accountName);
        if (limitResult.isExceeded()) {
            Log.warnf("[%s#%d] Budget exhausted - skipping walkthrough", repoName, prNumber);
            return;
        }

        String diff = gitHubService.fetchDiff(pullRequest);
        if (diff == null || diff.isBlank()) {
            Log.warnf("[%s#%d] Empty diff - skipping walkthrough", repoName, prNumber);
            return;
        }

        Log.infof("[%s#%d] Sending %d char diff to AI for walkthrough", repoName, prNumber, diff.length());
        String walkthrough = walkthroughAI.walkthrough(diff);

        if (walkthrough == null || walkthrough.isBlank()) {
            Log.warnf("[%s#%d] AI returned empty walkthrough", repoName, prNumber);
            return;
        }

        rateLimitService.record(installationId);
        gitHubService.postReviewComment(pullRequest, walkthrough);
        Log.infof("[%s#%d] Walkthrough posted", repoName, prNumber);
    }
}
