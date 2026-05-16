package dev.rishabkumar.ai;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GeminiReviewService {

    @Inject
    CodeReviewAI codeReviewAI;

    public String review(String diff) {
        if (diff == null || diff.isBlank()) {
            Log.warn("Empty or null diff received, skipping review");
            return "No diff available to review.";
        }

        Log.info("Sending diff to Gemini for review");
        String result = codeReviewAI.reviewCode(diff);
        Log.info("Gemini review completed");
        return result;
    }
}
