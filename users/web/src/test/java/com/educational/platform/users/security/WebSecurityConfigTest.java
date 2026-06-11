package com.educational.platform.users.security;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;

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
}
