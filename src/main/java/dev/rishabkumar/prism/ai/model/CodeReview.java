package dev.rishabkumar.prism.ai.model;

import java.util.List;

public record CodeReview(
        String summary,
        int score,
        String severity,
        int bugCount,
        int securityCount,
        int performanceCount,
        int codeQualityCount,
        List<String> highlights,
        String recommendation,
        String fullReview
) {}
