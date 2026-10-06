package com.educational.platform.course.enrollments.api;

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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Represents API tests for course enrollments functionality.
 */
@Sql(scripts = "classpath:insert_data.sql")
@TestPropertySource("classpath:application-security.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CourseEnrollmentApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void register_validCourse_createdWithUUID() {
        var token = SignUpHelper.signUpStudent();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"student\": \"username\"\n" +
                        "}")

                .when()
                .post("/courses/{uuid}/course-enrollments", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void register_anonymousRequest_forbidden() {
        given()
                .contentType(ContentType.JSON)
                .body("{\n" +
                        "  \"student\": \"username\"\n" +
                        "}")

                .when()
                .post("/courses/{uuid}/course-enrollments", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void learningFlow_enrollCompleteLecturesArchiveRestore_progressPersisted() {
        var token = SignUpHelper.signUpStudent();
        var course = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        var firstLecture = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
        var secondLecture = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

        var enrollment = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"student\": \"username\"}")
                .when()
                .post("/courses/{uuid}/course-enrollments", course)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().asString().replace("\"", "");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/courses/{uuid}/course-enrollments/current", course)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", equalTo(enrollment))
                .body("courseName", equalTo("Java Basics"))
                .body("totalLectures", is(2))
                .body("progressPercent", is(0));

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments?status=IN_PROGRESS&page=0&size=5")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("items[0].uuid", equalTo(enrollment))
                .body("totalElements", is(1))
                .body("counts.inProgress", is(1))
                .body("counts.completed", is(0));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"completed\": true}")
                .when()
                .put("/course-enrollments/{uuid}/lectures/{lecture}/progress", enrollment, firstLecture)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("enrollment.progressPercent", is(50))
                .body("enrollment.completionStatus", equalTo("IN_PROGRESS"))
                .body("lectures[0].completed", is(true))
                .body("lectures[1].completed", is(false));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"completed\": true}")
                .when()
                .put("/course-enrollments/{uuid}/lectures/{lecture}/progress", enrollment, secondLecture)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("enrollment.completionStatus", equalTo("COMPLETED"))
                .body("enrollment.progressPercent", is(100));

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments/{uuid}", enrollment)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("enrollment.completedLectures", is(2))
                .body("lectures", hasSize(2));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"archived\": true}")
                .when()
                .put("/course-enrollments/{uuid}/archive-status", enrollment)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("archived", is(true));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"completed\": false}")
                .when()
                .put("/course-enrollments/{uuid}/lectures/{lecture}/progress", enrollment, secondLecture)
                .then()
                .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments?status=ARCHIVED")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("counts.archived", is(1));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"archived\": false}")
                .when()
                .put("/course-enrollments/{uuid}/archive-status", enrollment)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("archived", is(false))
                .body("completionStatus", equalTo("COMPLETED"));
    }

    @Test
    void currentEnrollment_notEnrolled_notFound() {
        var token = SignUpHelper.signUpStudent();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/courses/{uuid}/course-enrollments/current", UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void enrollment_unknownUuid_notFound() {
        var token = SignUpHelper.signUpStudent();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments/{uuid}", UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void enrollments_anonymousRequest_forbidden() {
        given()
                .when()
                .get("/course-enrollments")

                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void enrollments_pageSizeAboveMaxAndNegativePage_clampedToLimits() {
        var token = SignUpHelper.signUpStudent();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments?page=-1&size=500")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("page", is(0))
                .body("size", is(50))
                .body("items", hasSize(0))
                .body("totalElements", is(0))
                .body("totalPages", is(0))
                .body("counts.inProgress", is(0))
                .body("counts.completed", is(0))
                .body("counts.archived", is(0));
    }

    @Test
    void enrollments_noParameters_defaultPageSize() {
        var token = SignUpHelper.signUpStudent();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("page", is(0))
                .body("size", is(12));
    }

    @Test
    void updateLectureProgress_lectureNotInCourse_notFound() {
        var token = SignUpHelper.signUpStudent();
        var course = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        var enrollment = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"student\": \"username\"}")
                .when()
                .post("/courses/{uuid}/course-enrollments", course)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().asString().replace("\"", "");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"completed\": true}")
                .when()
                .put("/course-enrollments/{uuid}/lectures/{lecture}/progress", enrollment, UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void updateArchiveStatus_unknownEnrollment_notFound() {
        var token = SignUpHelper.signUpStudent();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"archived\": true}")
                .when()
                .put("/course-enrollments/{uuid}/archive-status", UUID.randomUUID())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

}
