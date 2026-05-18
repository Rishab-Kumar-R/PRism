package dev.rishabkumar.prism.ai.service;

import dev.rishabkumar.prism.ai.client.CodeReviewAI;
import dev.rishabkumar.prism.ai.model.CodeReview;
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
        CodeReview result = aiReviewService.review(null);

        assertNull(result);
        verifyNoInteractions(codeReviewAI);
    }

    @Test
    void review_whenDiffIsBlank_returnsNullWithoutCallingAI() {
        CodeReview result = aiReviewService.review("   ");

        assertNull(result);
        verifyNoInteractions(codeReviewAI);
    }

    @Test
    void review_whenDiffIsEmpty_returnsNullWithoutCallingAI() {
        CodeReview result = aiReviewService.review("");

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

        CodeReview result = aiReviewService.review("valid diff");

        assertNotNull(result);
        assertEquals("APPROVED", result.severity());
        assertEquals(8, result.score());
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

        CodeReview result = aiReviewService.review("diff");

        assertNotNull(result);
        assertEquals("NEEDS_WORK", result.severity());
        assertEquals(5, result.score());
        assertEquals("Test summary", result.summary());
        assertEquals(2, result.bugCount());
        assertEquals(1, result.securityCount());
        assertEquals("Fix the null pointer", result.recommendation());
        assertEquals("## Review\nLooks risky.", result.fullReview());
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
