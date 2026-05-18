package dev.rishabkumar.prism.ai.service;

import dev.rishabkumar.prism.ai.client.CodeReviewAI;
import dev.rishabkumar.prism.ai.model.CodeReview;
import dev.rishabkumar.prism.ai.model.ReviewOutcome;
import dev.rishabkumar.prism.exception.AIReviewException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class AIReviewService {

    @ConfigProperty(name = "app.review.max-diff-chars", defaultValue = "50000")
    int maxDiffChars;

    @Inject
    CodeReviewAI codeReviewAI;

    public ReviewOutcome review(String diff) {
        return review(diff, null);
    }

    public ReviewOutcome review(String diff, String previousContext) {
        if (diff == null || diff.isBlank()) {
            Log.warn("Empty or null diff received, skipping review");
            return null;
        }

        if (diff.length() <= maxDiffChars) {
            Log.info("Sending diff to AI as single chunk");
            return ReviewOutcome.single(callAI(buildPrompt(diff, previousContext)));
        }

        List<String> fileDiffs = DiffSplitter.splitByFile(diff);
        List<String> chunks = DiffSplitter.groupIntoChunks(fileDiffs, maxDiffChars);
        Log.infof("Large diff (%d chars) split into %d chunks across %d files", diff.length(), chunks.size(), fileDiffs.size());

        List<CodeReview> chunkReviews = new java.util.ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Log.infof("Reviewing chunk %d/%d (%d chars)", i + 1, chunks.size(), chunks.get(i).length());
            String prompt = i == 0
                    ? buildPrompt(chunks.get(i), previousContext)
                    : buildChunkPrompt(chunks.get(i), i + 1, chunks.size());
            chunkReviews.add(callAI(prompt));
        }

        return ReviewOutcome.chunked(merge(chunkReviews));
    }

    private CodeReview callAI(String prompt) {
        CodeReview result = codeReviewAI.reviewCode(prompt);
        if (result == null) {
            Log.warn("AI returned null review");
            throw new AIReviewException("AI returned null review for provided diff");
        }

        Log.infof("AI review completed: score=%d severity=%s", result.score(), result.severity());
        return result;
    }

    private String buildPrompt(String diff, String previousContext) {
        if (previousContext != null && !previousContext.isBlank()) {
            return """
                    Previous review findings:
                    %s

                    Current diff to review:
                    %s

                    Where relevant, acknowledge issues from the previous review that have been fixed.
                    Re-flag any that still persist.
                    """.formatted(previousContext, diff);
        }
        return diff;
    }

    private String buildChunkPrompt(String diff, int part, int total) {
        return """
                This is part %d of %d of a large pull request diff. Review only the files in this chunk.
                Produce a complete structured review for these files.

                %s
                """.formatted(part, total, diff);
    }

    private CodeReview merge(List<CodeReview> reviews) {
        int minScore = reviews.stream().mapToInt(CodeReview::score).min().orElse(5);
        boolean needsWork = reviews.stream().anyMatch(r -> "NEEDS_WORK".equals(r.severity()));

        int bugs = reviews.stream().mapToInt(CodeReview::bugCount).sum();
        int security = reviews.stream().mapToInt(CodeReview::securityCount).sum();
        int perf = reviews.stream().mapToInt(CodeReview::performanceCount).sum();
        int quality = reviews.stream().mapToInt(CodeReview::codeQualityCount).sum();

        List<String> highlights = reviews.stream()
                .flatMap(r -> r.highlights().stream())
                .distinct()
                .limit(8)
                .toList();

        CodeReview worst = reviews.stream()
                .min(Comparator.comparingInt(CodeReview::score))
                .orElse(reviews.get(0));

        String summary = "Reviewed %d file groups. %s".formatted(reviews.size(), reviews.get(0).summary());

        StringBuilder fullReview = new StringBuilder();
        for (int i = 0; i < reviews.size(); i++) {
            fullReview.append("### Part ").append(i + 1).append(" of ").append(reviews.size()).append("\n\n");
            fullReview.append(reviews.get(i).fullReview()).append("\n\n");
        }

        return new CodeReview(
                summary,
                minScore,
                needsWork ? "NEEDS_WORK" : "APPROVED",
                bugs, security, perf, quality,
                highlights,
                worst.recommendation(),
                fullReview.toString().trim()
        );
    }
}
