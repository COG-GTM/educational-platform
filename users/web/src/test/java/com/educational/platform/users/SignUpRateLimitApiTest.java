package com.educational.platform.users;

import com.educational.platform.web.handler.ErrorResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Represents API tests for sign-up throttling (filter wired into the servlet chain with security enabled).
 */
@TestPropertySource(locations = "classpath:application-security.properties", properties = {
        "com.educational.platform.security.sign-up.rate-limit.max-requests=2",
        "com.educational.platform.security.sign-up.rate-limit.window=PT1H"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SignUpRateLimitApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    private static String signUpBody(String username) {
        return "{\n" + "    \"role\": \"ROLE_STUDENT\",\n" + "    \"username\": \"" + username + "\",\n" + "    \"email\": \"" + username + "@gmail.com\",\n" + "    \"password\": \"password\"\n" + "}";
    }

    @Test
    void signUp_limitExceeded_tooManyRequestsAndOtherEndpointsUnaffected() {
        given()
                .contentType(ContentType.JSON)
                .body(signUpBody("throttled_one"))

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(ContentType.JSON)
                .body(signUpBody("throttled_two"))

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(HttpStatus.OK.value());

        final ErrorResponse rejected = given()
                .contentType(ContentType.JSON)
                .body(signUpBody("throttled_three"))

                .when()
                .post("/users/sign-up")

                .then()
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .contentType(ContentType.JSON)
                .header("Retry-After", matchesPattern("\\d+"))
                .body("errors", contains("Too many sign-up attempts, please try again later"))
                .extract().as(ErrorResponse.class);
        assertThat(rejected.errors()).hasSize(1);

        given()
                .contentType(ContentType.JSON)
                .body("{\n" + "    \"username\": \"throttled_one\",\n" + "    \"password\": \"password\"\n" + "}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(ContentType.JSON)
                .body("{\n" + "    \"username\": \"throttled_three\",\n" + "    \"password\": \"password\"\n" + "}")

                .when()
                .post("/users/sign-in")

                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }
}
