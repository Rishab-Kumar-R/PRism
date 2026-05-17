package dev.rishabkumar.prism.ai;

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
    void review_whenAIReturnsNull_returnsNull() {
        when(codeReviewAI.reviewCode(anyString())).thenReturn(null);

        CodeReview result = aiReviewService.review("diff content");

        assertNull(result);
    }

    @Test
    void review_whenValidDiff_returnsCodeReview() {
        CodeReview mockReview = buildMockReview("APPROVED", 8);
        when(codeReviewAI.reviewCode("valid diff")).thenReturn(mockReview);

        CodeReview result = aiReviewService.review("valid diff");

        assertNotNull(result);
        assertEquals("APPROVED", result.getSeverity());
        assertEquals(8, result.getScore());
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
        assertEquals("NEEDS_WORK", result.getSeverity());
        assertEquals(5, result.getScore());
        assertEquals("Test summary", result.getSummary());
        assertEquals(2, result.getBugCount());
        assertEquals(1, result.getSecurityCount());
        assertEquals("Fix the null pointer", result.getRecommendation());
        assertEquals("## Review\nLooks risky.", result.getFullReview());
    }

    private CodeReview buildMockReview(String severity, int score) {
        CodeReview review = new CodeReview();

        review.setSeverity(severity);
        review.setScore(score);
        review.setSummary("Test summary");
        review.setBugCount(2);
        review.setSecurityCount(1);
        review.setPerformanceCount(0);
        review.setCodeQualityCount(3);
        review.setHighlights(List.of("Issue 1", "Issue 2"));
        review.setRecommendation("Fix the null pointer");
        review.setFullReview("## Review\nLooks risky.");

        return review;
    }
}
