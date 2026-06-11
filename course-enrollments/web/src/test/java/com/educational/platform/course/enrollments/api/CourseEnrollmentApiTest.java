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
    void register_noAuth_unauthorized() {
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
    void listCourseEnrollments_noAuth_unauthorized() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/course-enrollments")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void listCourseEnrollments_studentAuthenticated_ok() {
        var token = SignUpHelper.signUpStudent();

        // first register an enrollment
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

        // then list enrollments
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)

                .when()
                .get("/course-enrollments")

                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void listCourseEnrollments_studentAuthenticated_returnsNonEmptyArray() {
        var token = SignUpHelper.signUpStudent();

        // register an enrollment first
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

        // then list and verify body
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", org.hamcrest.Matchers.greaterThanOrEqualTo(1));
    }

    @Test
    void listCourseEnrollments_studentAuthenticated_noEnrollments_returnsEmptyArray() {
        var token = SignUpHelper.signUpStudent();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", org.hamcrest.Matchers.equalTo(0));
    }

    @Test
    void register_validCourse_returnsUuidInBody() {
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
                .statusCode(HttpStatus.CREATED.value())
                .body(org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void register_nonExistentCourse_badRequest() {
        var token = SignUpHelper.signUpStudent();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"student\": \"username\"\n" +
                        "}")
                .when()
                .post("/courses/{uuid}/course-enrollments", UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void register_validCourse_responseBodyIsValidUuidFormat() {
        var token = SignUpHelper.signUpStudent();

        var body = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"student\": \"username\"\n" +
                        "}")
                .when()
                .post("/courses/{uuid}/course-enrollments", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().body().asString();

        // then — response body is a valid UUID
        var parsed = UUID.fromString(body.replace("\"", ""));
        org.assertj.core.api.Assertions.assertThat(parsed).isNotNull();
        org.assertj.core.api.Assertions.assertThat(parsed.toString())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void listCourseEnrollments_studentAuthenticated_responseContainsDTOFields() {
        var token = SignUpHelper.signUpStudent();

        // register an enrollment first
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

        // then verify the response DTO structure
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/course-enrollments")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("[0].uuid", org.hamcrest.Matchers.notNullValue())
                .body("[0].course", org.hamcrest.Matchers.notNullValue())
                .body("[0].student", org.hamcrest.Matchers.equalTo("username"))
                .body("[0].completionStatus", org.hamcrest.Matchers.equalTo("IN_PROGRESS"));
    }

    @Test
    void register_invalidToken_unauthorized() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer invalid-token-value")
                .body("{\n" +
                        "  \"student\": \"username\"\n" +
                        "}")
                .when()
                .post("/courses/{uuid}/course-enrollments", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void listCourseEnrollments_invalidToken_unauthorized() {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer invalid-token-value")
                .when()
                .get("/course-enrollments")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

}
