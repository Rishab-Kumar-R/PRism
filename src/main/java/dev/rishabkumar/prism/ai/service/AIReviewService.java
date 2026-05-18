package dev.rishabkumar.prism.ai.service;

import dev.rishabkumar.prism.ai.client.CodeReviewAI;
import dev.rishabkumar.prism.ai.model.CodeReview;
import dev.rishabkumar.prism.exception.AIReviewException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AIReviewService {

    @Inject
    CodeReviewAI codeReviewAI;

    public CodeReview review(String diff) {
        return review(diff, null);
    }

    public CodeReview review(String diff, String previousContext) {
        if (diff == null || diff.isBlank()) {
            Log.warn("Empty or null diff received, skipping review");
            return null;
        }

        String prompt;
        if (previousContext != null && !previousContext.isBlank()) {
            Log.info("Sending diff to AI with previous review context");
            prompt = """
                    Previous review findings:
                    %s

                    Current diff to review:
                    %s

                    Where relevant, acknowledge issues from the previous review that have been fixed.
                    Re-flag any that still persist.
                    """.formatted(previousContext, diff);
        } else {
            Log.info("Sending diff to AI for first review");
            prompt = diff;
        }

        CodeReview result = codeReviewAI.reviewCode(prompt);

        if (result == null) {
            Log.warn("AI returned null review");
            throw new AIReviewException("AI returned null review for provided diff");
        }

        Log.infof("AI review completed: score=%d severity=%s", result.score(), result.severity());
        return result;
    }
}
