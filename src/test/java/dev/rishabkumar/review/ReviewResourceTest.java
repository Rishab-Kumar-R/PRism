package dev.rishabkumar.review;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
public class ReviewResourceTest {

    @Inject
    ReviewRepository reviewRepository;

    @Inject
    UserTransaction userTransaction;

    private static final String VALID_API_KEY = "test-api-key";
    private static final String WRONG_API_KEY = "wrong-key";

    @BeforeEach
    void cleanup() throws Exception {
        userTransaction.begin();
        reviewRepository.deleteAll();
        userTransaction.commit();
    }

    @Test
    void getAllReviews_withoutApiKey_returns401() {
        given()
                .when().get("/reviews")
                .then().statusCode(401);
    }

    @Test
    void getAllReviews_withWrongApiKey_returns401() {
        given()
                .header("API-Key", WRONG_API_KEY)
                .when().get("/reviews")
                .then().statusCode(401);
    }

    @Test
    void getAllReviews_withValidApiKey_returns200() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews")
                .then().statusCode(200);
    }

    @Test
    void getStats_withoutApiKey_returns401() {
        given()
                .when().get("/reviews/stats")
                .then().statusCode(401);
    }

    @Test
    void getById_withoutApiKey_returns401() {
        given()
                .when().get("/reviews/1")
                .then().statusCode(401);
    }

    @Test
    void getAllReviews_whenEmpty_returnsEmptyList() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    void getAllReviews_returnsPersistedReviews() throws Exception {
        persist(
                buildRecord("repo/a", 1, "sha1", "APPROVED", 8),
                buildRecord("repo/a", 2, "sha2", "NEEDS_WORK", 4)
        );

        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews")
                .then()
                .statusCode(200)
                .body("", hasSize(2));
    }

    @Test
    void getAllReviews_paginationWorks() throws Exception {
        ReviewRecord[] records = new ReviewRecord[25];
        for (int i = 0; i < 25; i++) {
            records[i] = buildRecord("repo/a", i + 1, "sha" + i, "APPROVED", 8);
        }
        persist(records);

        given()
                .header("API-Key", VALID_API_KEY)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when().get("/reviews")
                .then()
                .statusCode(200)
                .body("", hasSize(10));

        given()
                .header("API-Key", VALID_API_KEY)
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when().get("/reviews")
                .then()
                .statusCode(200)
                .body("", hasSize(5));
    }

    @Test
    void getById_whenExists_returnsReview() throws Exception {
        ReviewRecord record = buildRecord("repo/a", 1, "sha1", "APPROVED", 9);
        persist(record);

        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/" + record.id)
                .then()
                .statusCode(200)
                .body("repoName", equalTo("repo/a"))
                .body("prNumber", equalTo(1))
                .body("severity", equalTo("APPROVED"))
                .body("score", equalTo(9));
    }

    @Test
    void getById_whenNotExists_returns404() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/999999")
                .then()
                .statusCode(404);
    }

    @Test
    void getByRepo_returnsOnlyMatchingRepo() throws Exception {
        persist(
                buildRecord("repo/a", 1, "sha1", "APPROVED", 8),
                buildRecord("repo/a", 2, "sha2", "APPROVED", 7),
                buildRecord("repo/b", 1, "sha3", "NEEDS_WORK", 4)
        );

        given()
                .urlEncodingEnabled(false)
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/repo/repo%2Fa")
                .then()
                .statusCode(200)
                .body("", hasSize(2))
                .body("repoName", everyItem(equalTo("repo/a")));
    }

    @Test
    void getByRepo_whenNoMatch_returnsEmptyList() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/repo/nonexistent")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    void getByPr_returnsOnlyMatchingPrNumber() throws Exception {
        persist(
                buildRecord("repo/a", 42, "sha1", "APPROVED", 8),
                buildRecord("repo/a", 42, "sha2", "NEEDS_WORK", 5),
                buildRecord("repo/a", 99, "sha3", "APPROVED", 9)
        );

        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/pr/42")
                .then()
                .statusCode(200)
                .body("", hasSize(2))
                .body("prNumber", everyItem(equalTo(42)));
    }

    @Test
    void getStats_returnsCorrectTotals() throws Exception {
        persist(
                buildRecord("repo/a", 1, "sha1", "APPROVED", 8),
                buildRecord("repo/a", 2, "sha2", "APPROVED", 9),
                buildRecord("repo/b", 1, "sha3", "NEEDS_WORK", 4)
        );

        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/stats")
                .then()
                .statusCode(200)
                .body("totalReviews", equalTo(3))
                .body("approved", equalTo(2))
                .body("needsWork", equalTo(1))
                .body("approvalRate", equalTo("67%"))
                .body("mostReviewedRepo", equalTo("repo/a"));
    }

    @Test
    void getStats_whenEmpty_returnsZeroValues() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/reviews/stats")
                .then()
                .statusCode(200)
                .body("totalReviews", equalTo(0))
                .body("approvalRate", equalTo("0%"))
                .body("mostReviewedRepo", equalTo("none"));
    }

    private void persist(ReviewRecord... records) throws Exception {
        userTransaction.begin();
        for (ReviewRecord record : records) {
            reviewRepository.persist(record);
        }
        userTransaction.commit();
    }

    private ReviewRecord buildRecord(String repoName, int prNumber, String commitSha,
                                     String severity, int score) {
        return new ReviewRecord(repoName, prNumber, "PR Title", commitSha,
                severity, score, 1, 0, 0, 0,
                "Fix something", "## Review content");
    }
}
