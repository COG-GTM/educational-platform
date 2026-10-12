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
 * Verifies the "fully off" switch documented in the README: disabling springdoc removes the API docs and Swagger UI
 * even when the local-development opt-in flag would otherwise expose them anonymously.
 * <p>
 * Only "not served" is asserted: unmatched paths currently surface as 500 rather than 404 because
 * {@code GlobalExceptionHandler#onException} catches {@code NoResourceFoundException}, which is outside this change.
 */
@TestPropertySource(locations = "classpath:application-security.properties",
        properties = {
                "com.educational.platform.security.api-docs.public=true",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false"
        })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiDocsDisabledTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html"})
    void apiDocs_springdocDisabled_notServedEvenWhenOptedInAsPublic(String path) {
        given()
                .redirects().follow(false)
                .when()
                .get(path)
                .then()
                .statusCode(not(HttpStatus.OK.value()));
    }
}
