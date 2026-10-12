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

    private String signUp() {
        var username = "csrf" + UUID.randomUUID().toString().substring(0, 8);
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
