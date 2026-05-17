package dev.rishabkumar.prism.review.service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;

@ApplicationScoped
public class ReviewMetrics {

    @Inject
    MeterRegistry registry;

    public void recordSuccess(String repoName, long durationMs) {
        registry.counter("prism.reviews.total", "repo", repoName, "result", "success").increment();
        registry.counter("prism.reviews.success").increment();
        registry.timer("prism.review.duration", "result", "success").record(Duration.ofMillis(durationMs));
    }

    public void recordError(String repoName, long durationMs) {
        registry.counter("prism.reviews.total", "repo", repoName, "result", "error").increment();
        registry.counter("prism.reviews.error").increment();
        registry.timer("prism.review.duration", "result", "error").record(Duration.ofMillis(durationMs));
    }

    public void recordSkipped(String reason) {
        registry.counter("prism.reviews.skipped", "reason", reason).increment();
    }
}
