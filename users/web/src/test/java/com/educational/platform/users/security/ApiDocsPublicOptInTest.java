package com.educational.platform.users.security;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
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
    @CsvSource({"/v3/api-docs, application/json", "/swagger-ui/index.html, text/html"})
    void apiDocs_anonymous_accessibleWhenOptedIn(String path, String contentType) {
        given()
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(containsString(contentType));
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
