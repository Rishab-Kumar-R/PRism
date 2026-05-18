package dev.rishabkumar.prism.ai.service;

import dev.rishabkumar.prism.ai.client.CodeReviewAI;
import dev.rishabkumar.prism.ai.model.CodeReview;
import dev.rishabkumar.prism.ai.model.ReviewOutcome;
import dev.rishabkumar.prism.exception.AIReviewException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AIReviewServiceTest {

    @Inject
    AIReviewService aiReviewService;

    @InjectMock
    CodeReviewAI codeReviewAI;

    @Test
    void review_whenDiffIsNull_returnsNullWithoutCallingAI() {
        ReviewOutcome result = aiReviewService.review(null);

        assertNull(result);
        verifyNoInteractions(codeReviewAI);
    }

    @Test
    void review_whenDiffIsBlank_returnsNullWithoutCallingAI() {
        ReviewOutcome result = aiReviewService.review("   ");

        assertNull(result);
        verifyNoInteractions(codeReviewAI);
    }

    @Test
    void review_whenDiffIsEmpty_returnsNullWithoutCallingAI() {
        ReviewOutcome result = aiReviewService.review("");

        assertNull(result);
        verifyNoInteractions(codeReviewAI);
    }

    @Test
    void review_whenAIReturnsNull_throwsAIReviewException() {
        when(codeReviewAI.reviewCode(anyString())).thenReturn(null);

        assertThrows(AIReviewException.class, () -> aiReviewService.review("diff content"));
    }

    @Test
    void review_whenValidDiff_returnsReviewResult() {
        CodeReview mockReview = buildMockReview("APPROVED", 8);
        when(codeReviewAI.reviewCode("valid diff")).thenReturn(mockReview);

        ReviewOutcome outcome = aiReviewService.review("valid diff");

        assertNotNull(outcome);
        assertEquals("APPROVED", outcome.review().severity());
        assertEquals(8, outcome.review().score());
        assertFalse(outcome.chunked());
    }

    @Test
    void review_whenValidDiff_callsAIExactlyOnce() {
        when(codeReviewAI.reviewCode(anyString())).thenReturn(buildMockReview("NEEDS_WORK", 4));

        aiReviewService.review("some diff");

        verify(codeReviewAI, times(1)).reviewCode("some diff");
    }

    @Test
    void review_returnsAllFieldsFromAIResponse() {
        CodeReview mockReview = buildMockReview("NEEDS_WORK", 5);
        when(codeReviewAI.reviewCode(anyString())).thenReturn(mockReview);

        ReviewOutcome outcome = aiReviewService.review("diff");
        CodeReview result = outcome.review();

        assertNotNull(result);
        assertEquals("NEEDS_WORK", result.severity());
        assertEquals(5, result.score());
        assertEquals("Test summary", result.summary());
        assertEquals(2, result.bugCount());
        assertEquals(1, result.securityCount());
        assertEquals("Fix the null pointer", result.recommendation());
        assertEquals("## Review\nLooks risky.", result.fullReview());
    }

    @Test
    void review_whenDiffExceedsLimit_reviewsInChunksAndMerges() {
        // Two file diffs that together exceed the 50K char default limit
        String fileA = "diff --git a/A.java b/A.java\n" + "x".repeat(30_000);
        String fileB = "diff --git a/B.java b/B.java\n" + "y".repeat(30_000);
        String largeDiff = fileA + fileB;

        CodeReview chunkReview = buildMockReview("APPROVED", 8);
        when(codeReviewAI.reviewCode(anyString())).thenReturn(chunkReview);

        ReviewOutcome outcome = aiReviewService.review(largeDiff);

        assertTrue(outcome.chunked());
        verify(codeReviewAI, times(2)).reviewCode(anyString());
    }

    @Test
    void review_whenChunked_takesMinScore() {
        String fileA = "diff --git a/A.java b/A.java\n" + "x".repeat(30_000);
        String fileB = "diff --git a/B.java b/B.java\n" + "y".repeat(30_000);
        String largeDiff = fileA + fileB;

        when(codeReviewAI.reviewCode(anyString()))
                .thenReturn(buildMockReview("APPROVED", 9))
                .thenReturn(buildMockReview("NEEDS_WORK", 3));

        ReviewOutcome outcome = aiReviewService.review(largeDiff);

        assertEquals(3, outcome.review().score());
        assertEquals("NEEDS_WORK", outcome.review().severity());
    }

    @Test
    void review_whenChunked_sumsCounts() {
        String fileA = "diff --git a/A.java b/A.java\n" + "x".repeat(30_000);
        String fileB = "diff --git a/B.java b/B.java\n" + "y".repeat(30_000);
        String largeDiff = fileA + fileB;

        when(codeReviewAI.reviewCode(anyString()))
                .thenReturn(buildMockReview("APPROVED", 8))  // bugCount=2, securityCount=1
                .thenReturn(buildMockReview("APPROVED", 7)); // bugCount=2, securityCount=1

        ReviewOutcome outcome = aiReviewService.review(largeDiff);

        assertEquals(4, outcome.review().bugCount());
        assertEquals(2, outcome.review().securityCount());
    }

    private CodeReview buildMockReview(String severity, int score) {
        return new CodeReview(
                "Test summary",
                score,
                severity,
                2,
                1,
                0,
                3,
                List.of("Issue 1", "Issue 2"),
                "Fix the null pointer",
                "## Review\nLooks risky."
        );
    }
}
