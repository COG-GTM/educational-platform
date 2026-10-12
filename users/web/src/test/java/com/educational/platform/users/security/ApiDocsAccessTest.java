package com.educational.platform.users.security;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.oneOf;

/**
 * Verifies that API docs and Swagger UI require authentication by default.
 */
@TestPropertySource("classpath:application-security.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiDocsAccessTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html",
            "/v2/api-docs", "/swagger-resources", "/configuration/ui"})
    void apiDocs_anonymous_rejected(String path) {
        given()
                .redirects().follow(false)
                .when()
                .get(path)
                .then()
                .statusCode(oneOf(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void apiDocs_authenticated_accessible(String path) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"role\": \"ROLE_STUDENT\", \"username\": \"docs-user\", \"email\": \"docs@gmail.com\", \"password\": \"password\"}")
                .post("/users/sign-up");
        String token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"docs-user\", \"password\": \"password\"}")
                .post("/users/sign-in")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().path("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
