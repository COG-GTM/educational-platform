package com.educational.platform.users.security;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

/**
 * Verifies the opt-in flag that exposes API docs and Swagger UI anonymously (local development only).
 */
@TestPropertySource(locations = "classpath:application-security.properties",
        properties = "com.educational.platform.security.api-docs.public=true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiDocsPublicOptInTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui/index.html"})
    void apiDocs_anonymous_accessibleWhenOptedIn(String path) {
        given()
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/courses", "/v2/api-docs"})
    void otherEndpoints_anonymous_stillRejected(String path) {
        given()
                .redirects().follow(false)
                .when()
                .get(path)
                .then()
                .statusCode(not(HttpStatus.OK.value()));
    }
}
