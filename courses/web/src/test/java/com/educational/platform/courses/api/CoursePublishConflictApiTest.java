package com.educational.platform.courses.api;

import com.educational.platform.security.SignUpHelper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Represents API tests for course publish conflict scenarios.
 */
@Sql(scripts = "classpath:insert_non_approved_course.sql")
@TestPropertySource("classpath:application-security.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CoursePublishConflictApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void publish_nonApprovedCourse_conflict() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)

                .when()
                .put("/courses/{uuid}/publish-status", UUID.fromString("123e4567-e89b-12d3-a456-426655440002"))

                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }
}
