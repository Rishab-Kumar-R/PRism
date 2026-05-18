package dev.rishabkumar.prism.ai.model;

public record ReviewOutcome(CodeReview review, boolean chunked) {

    public static ReviewOutcome single(CodeReview review) {
        return new ReviewOutcome(review, false);
    }

    public static ReviewOutcome chunked(CodeReview review) {
        return new ReviewOutcome(review, true);
    }
}
