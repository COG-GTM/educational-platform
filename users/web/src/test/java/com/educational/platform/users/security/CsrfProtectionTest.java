package com.educational.platform.users.security;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;

/**
 * Verifies CSRF protection is enabled for cookie/browser style requests while Bearer-token API calls keep working.
 */
@TestPropertySource("classpath:application-security.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CsrfProtectionTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void post_withoutCsrfToken_forbidden() {
        given()
                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withMismatchedCsrfToken_forbidden() {
        given()
                .cookie("XSRF-TOKEN", "cookie-token")
                .header("X-XSRF-TOKEN", "other-token")

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withMatchingCsrfCookieAndHeader_allowed() {
        var token = UUID.randomUUID().toString();
        given()
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_withMatchingCsrfCookieAndFormParameter_allowed() {
        var token = UUID.randomUUID().toString();
        given()
                .cookie("XSRF-TOKEN", token)
                .formParam("_csrf", token)

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_withBearerToken_exemptFromCsrf() {
        var jwt = signUp();
        given()
                .header("Authorization", "Bearer " + jwt)

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_withEmptyBearerToken_stillRequiresCsrfToken() {
        given()
                .header("Authorization", "Bearer ")

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withCsrfCookieButNoHeader_forbidden() {
        given()
                .cookie("XSRF-TOKEN", UUID.randomUUID().toString())

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withCsrfHeaderButNoCookie_forbidden() {
        given()
                .header("X-XSRF-TOKEN", UUID.randomUUID().toString())

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withoutCsrfToken_issuesReadableXsrfCookie() {
        var cookie = given()
                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract()
                .detailedCookie("XSRF-TOKEN");

        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void post_withIssuedXsrfCookieEchoedInHeader_allowed() {
        var issued = given()
                .when()
                .post("/public")

                .then()
                .extract()
                .cookie("XSRF-TOKEN");

        given()
                .cookie("XSRF-TOKEN", issued)
                .header("X-XSRF-TOKEN", issued)

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void get_withoutCsrfToken_allowed() {
        given()
                .when()
                .get("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_withInvalidBearerToken_notAccepted() {
        given()
                .header("Authorization", "Bearer not-a-jwt")

                .when()
                .post("/public")

                .then()
                .statusCode(not(HttpStatus.OK.value()));
    }

    @Test
    void post_withNonBearerAuthorizationHeader_forbidden() {
        given()
                .header("Authorization", "Basic dXNlcjpwYXNz")

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_toAuthenticatedEndpoint_withBearerToken_allowedWithoutCsrf() {
        var jwt = signUp();
        given()
                .header("Authorization", "Bearer " + jwt)

                .when()
                .post("/protected")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_toAuthenticatedEndpoint_withCsrfTokenButNoBearer_notOk() {
        var token = UUID.randomUUID().toString();
        given()
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)

                .when()
                .post("/protected")

                .then()
                .statusCode(not(HttpStatus.OK.value()));
    }

    @Test
    void signIn_withoutCsrfToken_allowed() {
        var username = "csrf" + UUID.randomUUID().toString().substring(0, 8);
        signUp(username);

        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"username\": \"" + username + "\",\n" +
                        "  \"password\": \"password\"\n" +
                        "}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void h2Console_post_withoutCsrfToken_allowed() {
        given()
                .when()
                .post("/h2-console/probe")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void put_withoutCsrfToken_forbidden() {
        given()
                .when()
                .put("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void delete_withoutCsrfToken_forbidden() {
        given()
                .when()
                .delete("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void put_withMatchingCsrfCookieAndHeader_allowed() {
        var token = UUID.randomUUID().toString();
        given()
                .cookie("XSRF-TOKEN", token)
                .header("X-XSRF-TOKEN", token)

                .when()
                .put("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void delete_withBearerToken_exemptFromCsrf() {
        var jwt = signUp();
        given()
                .header("Authorization", "Bearer " + jwt)

                .when()
                .delete("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void head_withoutCsrfToken_allowed() {
        given()
                .when()
                .head("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void options_withoutCsrfToken_allowed() {
        given()
                .when()
                .options("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_withWhitespaceOnlyBearerToken_stillRequiresCsrfToken() {
        given()
                .header("Authorization", "Bearer    ")

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withLowercaseBearerScheme_forbidden() {
        var jwt = signUp();
        given()
                .header("Authorization", "bearer " + jwt)

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withMismatchedCsrfToken_keepsExistingCookie() {
        var cookie = given()
                .cookie("XSRF-TOKEN", "cookie-token")
                .header("X-XSRF-TOKEN", "other-token")

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .extract()
                .detailedCookie("XSRF-TOKEN");

        assertThat(cookie).isNull();
    }

    @Test
    void post_withMismatchedCsrfFormParameter_forbidden() {
        given()
                .cookie("XSRF-TOKEN", UUID.randomUUID().toString())
                .formParam("_csrf", UUID.randomUUID().toString())

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void post_withBearerTokenAndStaleCsrfCookie_allowed() {
        var jwt = signUp();
        given()
                .header("Authorization", "Bearer " + jwt)
                .cookie("XSRF-TOKEN", "stale-cookie-token")
                .header("X-XSRF-TOKEN", "other-token")

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void post_withBearerToken_doesNotIssueXsrfCookie() {
        var jwt = signUp();
        var cookie = given()
                .header("Authorization", "Bearer " + jwt)

                .when()
                .post("/public")

                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .cookie("XSRF-TOKEN");

        assertThat(cookie).isNull();
    }

    @Test
    void signUp_withFormContentType_withoutCsrfToken_rejectedAndNoAccountCreated() {
        var username = "csrfform" + UUID.randomUUID().toString().substring(0, 8);
        given()
                .contentType(ContentType.URLENC)
                .formParam("role", "ROLE_STUDENT")
                .formParam("username", username)
                .formParam("email", username + "@gmail.com")
                .formParam("password", "password")

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(not(HttpStatus.OK.value()));

        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"username\": \"" + username + "\",\n" +
                        "  \"password\": \"password\"\n" +
                        "}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(not(HttpStatus.OK.value()));
    }

    @Test
    void signIn_withTextPlainContentType_withoutCsrfToken_rejected() {
        var username = "csrf" + UUID.randomUUID().toString().substring(0, 8);
        signUp(username);

        given()
                .contentType(ContentType.TEXT)
                .body("{\"username\": \"" + username + "\", \"password\": \"password\"}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(not(HttpStatus.OK.value()));
    }

    private String signUp() {
        return signUp("csrf" + UUID.randomUUID().toString().substring(0, 8));
    }

    private String signUp(String username) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"role\": \"ROLE_STUDENT\",\n" +
                        "  \"username\": \"" + username + "\",\n" +
                        "  \"email\": \"" + username + "@gmail.com\",\n" +
                        "  \"password\": \"password\"\n" +
                        "}")

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .asString();
    }

}
