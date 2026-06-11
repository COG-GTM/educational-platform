package com.educational.platform.users.security;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "com.educational.platform.security.enabled=true"
)
public class WebSecurityConfigTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void signUpEndpoint_noAuth_accessible() {
        given()
                .contentType("application/json")
                .body("{\"role\": \"ROLE_STUDENT\", \"username\": \"secuser\", \"email\": \"sec@gmail.com\", \"password\": \"password\"}")

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void signInEndpoint_noAuth_accessible() {
        // first sign up
        given()
                .contentType("application/json")
                .body("{\"role\": \"ROLE_STUDENT\", \"username\": \"secuser2\", \"email\": \"sec2@gmail.com\", \"password\": \"password\"}")
                .post("/users/sign-up");

        given()
                .contentType("application/json")
                .body("{\"username\": \"secuser2\", \"password\": \"password\"}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void protectedEndpoint_noAuth_unauthorized() {
        given()
                .when()
                .get("/protected-resource")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void swaggerUiEndpoint_noAuth_accessible() {
        given()
                .when()
                .get("/swagger-ui/index.html")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void apiDocsEndpoint_noAuth_accessible() {
        given()
                .when()
                .get("/v3/api-docs")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void protectedEndpoint_postMethod_forbidden() {
        given()
                .contentType("application/json")
                .body("{}")

                .when()
                .post("/protected-resource")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void signUpEndpoint_invalidPayload_badRequest() {
        given()
                .contentType("application/json")
                .body("{}")

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void signInEndpoint_invalidPayload_badRequest() {
        given()
                .contentType("application/json")
                .body("{}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void h2ConsoleEndpoint_noAuth_notForbidden() {
        // h2-console is in the permitAll list — should not return 403
        final int status = given()
                .when()
                .get("/h2-console")
                .statusCode();

        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void protectedEndpoint_getMethod_forbidden() {
        given()
                .when()
                .get("/api/protected")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void publicEndpoint_noAuth_notForbidden() {
        final int status = given()
                .when()
                .get("/public")
                .statusCode();

        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void v2ApiDocsEndpoint_noAuth_notForbidden() {
        final int status = given()
                .when()
                .get("/v2/api-docs")
                .statusCode();

        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void swaggerResourcesEndpoint_noAuth_notForbidden() {
        final int status = given()
                .when()
                .get("/swagger-resources")
                .statusCode();

        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void webjarsEndpoint_noAuth_notForbidden() {
        final int status = given()
                .when()
                .get("/webjars/test")
                .statusCode();

        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void configurationEndpoint_noAuth_notForbidden() {
        final int status = given()
                .when()
                .get("/configuration/ui")
                .statusCode();

        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void protectedEndpoint_withValidJwt_accessible() {
        // first sign up a user
        final String token = given()
                .contentType("application/json")
                .body("{\"role\": \"ROLE_STUDENT\", \"username\": \"jwtuser\", \"email\": \"jwt@gmail.com\", \"password\": \"password\"}")
                .post("/users/sign-up")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .asString();

        // then access a protected endpoint with the JWT
        final int status = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/protected-resource")
                .statusCode();

        // should not be 403 Forbidden (the resource may not exist → 404, but should not be 403)
        assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void protectedEndpoint_withInvalidJwt_notSuccessful() {
        // invalid JWT should not grant access — exact status depends on Spring Security error dispatch
        final int status = given()
                .header("Authorization", "Bearer invalid-jwt-token")
                .when()
                .get("/protected-resource")
                .statusCode();

        assertThat(status).isGreaterThanOrEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void signUpEndpoint_getMethod_notAllowed() {
        // sign-up is mapped to POST only
        final int status = given()
                .when()
                .get("/users/sign-up")
                .statusCode();

        // GET is not mapped → should not return 200
        assertThat(status).isNotEqualTo(HttpStatus.OK.value());
    }
}
