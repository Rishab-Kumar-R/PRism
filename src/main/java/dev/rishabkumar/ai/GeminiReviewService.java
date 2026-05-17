package dev.rishabkumar.ai;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GeminiReviewService {

    @Inject
    CodeReviewAI codeReviewAI;

    public CodeReview review(String diff) {
        if (diff == null || diff.isBlank()) {
            Log.warn("Empty or null diff received, skipping review");
            return null;
        }

        Log.info("Sending diff to Gemini for structured review");
        CodeReview result = codeReviewAI.reviewCode(diff);

        if (result == null) {
            Log.warn("Gemini returned null review");
            return null;
        }

        Log.infof("Gemini review completed with score %d and severity %s",
                result.getScore(), result.getSeverity());
        return result;
    }
}
