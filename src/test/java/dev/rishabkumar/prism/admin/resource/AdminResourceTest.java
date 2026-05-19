package dev.rishabkumar.prism.admin.resource;

import dev.rishabkumar.prism.ratelimit.model.InstallationConfig;
import dev.rishabkumar.prism.ratelimit.model.Tier;
import dev.rishabkumar.prism.ratelimit.repository.InstallationConfigRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
public class AdminResourceTest {

    @Inject
    InstallationConfigRepository installationConfigRepository;

    @Inject
    UserTransaction userTransaction;

    private static final String VALID_API_KEY = "test-api-key";
    private static final String WRONG_API_KEY = "wrong-key";
    private static final long INSTALLATION_ID = 1001L;

    @BeforeEach
    void cleanup() throws Exception {
        userTransaction.begin();
        installationConfigRepository.delete("installationId", INSTALLATION_ID);
        userTransaction.commit();
    }

    // --- Auth ---

    @Test
    void listInstallations_withoutApiKey_returns401() {
        given()
                .when().get("/admin/installations")
                .then().statusCode(401);
    }

    @Test
    void listInstallations_withWrongApiKey_returns401() {
        given()
                .header("API-Key", WRONG_API_KEY)
                .when().get("/admin/installations")
                .then().statusCode(401);
    }

    @Test
    void getInstallation_withoutApiKey_returns401() {
        given()
                .when().get("/admin/installations/" + INSTALLATION_ID)
                .then().statusCode(401);
    }

    @Test
    void updateTier_withoutApiKey_returns401() {
        given()
                .contentType("application/json")
                .body("{\"tier\":\"PRO\"}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/tier")
                .then().statusCode(401);
    }

    @Test
    void updateLimits_withoutApiKey_returns401() {
        given()
                .contentType("application/json")
                .body("{\"customMonthlyReviewLimit\":100}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/limits")
                .then().statusCode(401);
    }

    // --- listInstallations ---

    @Test
    void listInstallations_whenEmpty_returnsEmptyList() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/admin/installations")
                .then().statusCode(200)
                .body("findAll { it.installationId == " + INSTALLATION_ID + " }", hasSize(0));
    }

    @Test
    void listInstallations_returnsAllInstallations() throws Exception {
        persist(new InstallationConfig(INSTALLATION_ID, "test-org"));

        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/admin/installations")
                .then().statusCode(200)
                .body("find { it.installationId == " + INSTALLATION_ID + " }.accountName", equalTo("test-org"));
    }

    @Test
    void listInstallations_filterByActive_returnsOnlyActive() throws Exception {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, "test-org");
        config.active = false;
        persist(config);

        given()
                .header("API-Key", VALID_API_KEY)
                .queryParam("active", true)
                .when().get("/admin/installations")
                .then().statusCode(200)
                .body("findAll { it.installationId == " + INSTALLATION_ID + " }", hasSize(0));
    }

    @Test
    void listInstallations_filterByInactive_returnsOnlyInactive() throws Exception {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, "test-org");
        config.active = false;
        persist(config);

        given()
                .header("API-Key", VALID_API_KEY)
                .queryParam("active", false)
                .when().get("/admin/installations")
                .then().statusCode(200)
                .body("find { it.installationId == " + INSTALLATION_ID + " }.active", equalTo(false));
    }

    // --- getInstallation ---

    @Test
    void getInstallation_whenExists_returnsInstallation() throws Exception {
        persist(new InstallationConfig(INSTALLATION_ID, "test-org"));

        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/admin/installations/" + INSTALLATION_ID)
                .then().statusCode(200)
                .body("installationId", equalTo((int) INSTALLATION_ID))
                .body("accountName", equalTo("test-org"))
                .body("tier", equalTo("FREE"))
                .body("active", equalTo(true));
    }

    @Test
    void getInstallation_whenNotFound_returns404() {
        given()
                .header("API-Key", VALID_API_KEY)
                .when().get("/admin/installations/99999")
                .then().statusCode(404);
    }

    // --- updateTier ---

    @Test
    void updateTier_upgradesCorrectly() throws Exception {
        persist(new InstallationConfig(INSTALLATION_ID, "test-org"));

        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"tier\":\"PRO\"}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/tier")
                .then().statusCode(200)
                .body("tier", equalTo("PRO"));
    }

    @Test
    void updateTier_downgradesCorrectly() throws Exception {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, "test-org");
        config.tier = Tier.TEAM;
        persist(config);

        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"tier\":\"FREE\"}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/tier")
                .then().statusCode(200)
                .body("tier", equalTo("FREE"));
    }

    @Test
    void updateTier_whenNotFound_returns404() {
        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"tier\":\"PRO\"}")
                .when().patch("/admin/installations/99999/tier")
                .then().statusCode(404);
    }

    @Test
    void updateTier_whenTierMissing_returns400() throws Exception {
        persist(new InstallationConfig(INSTALLATION_ID, "test-org"));

        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/tier")
                .then().statusCode(400);
    }

    // --- updateLimits ---

    @Test
    void updateLimits_setsCustomMonthlyLimit() throws Exception {
        persist(new InstallationConfig(INSTALLATION_ID, "test-org"));

        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"customMonthlyReviewLimit\":200,\"customDailyReviewLimit\":null}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/limits")
                .then().statusCode(200)
                .body("customMonthlyReviewLimit", equalTo(200));
    }

    @Test
    void updateLimits_setsCustomDailyLimit() throws Exception {
        persist(new InstallationConfig(INSTALLATION_ID, "test-org"));

        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"customMonthlyReviewLimit\":null,\"customDailyReviewLimit\":20}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/limits")
                .then().statusCode(200)
                .body("customDailyReviewLimit", equalTo(20));
    }

    @Test
    void updateLimits_clearsCustomLimitWithNull() throws Exception {
        InstallationConfig config = new InstallationConfig(INSTALLATION_ID, "test-org");
        config.customMonthlyReviewLimit = 300;
        persist(config);

        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"customMonthlyReviewLimit\":null,\"customDailyReviewLimit\":null}")
                .when().patch("/admin/installations/" + INSTALLATION_ID + "/limits")
                .then().statusCode(200)
                .body("customMonthlyReviewLimit", equalTo(null));
    }

    @Test
    void updateLimits_whenNotFound_returns404() {
        given()
                .header("API-Key", VALID_API_KEY)
                .contentType("application/json")
                .body("{\"customMonthlyReviewLimit\":100,\"customDailyReviewLimit\":null}")
                .when().patch("/admin/installations/99999/limits")
                .then().statusCode(404);
    }

    private void persist(InstallationConfig config) throws Exception {
        userTransaction.begin();
        installationConfigRepository.persist(config);
        userTransaction.commit();
    }
}
