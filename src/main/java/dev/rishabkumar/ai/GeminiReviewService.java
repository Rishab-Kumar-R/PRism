package dev.rishabkumar.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GeminiReviewService {

    @Inject
    CodeReviewAI codeReviewAI;

    public String review(String diff) {
        if (diff == null || diff.isBlank()) {
            return "No diff available to review.";
        }

        return codeReviewAI.reviewCode(diff);
    }
}
