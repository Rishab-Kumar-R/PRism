package dev.rishabkumar.prism.ai.model;

public record InlineComment(
        String path,
        int line,
        String body,
        String suggestion
) {
}
