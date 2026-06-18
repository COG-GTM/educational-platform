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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Represents API tests for course functionality.
 */
@Sql(scripts = "classpath:insert_data.sql")
@TestPropertySource("classpath:application-security.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CourseApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void create_validRequest_created() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"name\": \"name\",\n" +
                        "  \"description\": \"description\"\n" +
                        "}")

                .when()
                .post("/courses")

                .then()
                .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    void publish_alreadyApprovedCourse_noContent() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)

                .when()
                .put("/courses/{uuid}/publish-status", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void search_keywordMatchesExistingCourse_courseReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "course")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("course name"))
                .body("[0].description", equalTo("description"));
    }

    @Test
    void search_keywordWithoutMatches_emptyArrayReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "no-such-course")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }

    @Test
    void search_keywordMatchesDescription_courseReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "description")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("course name"))
                .body("[0].description", equalTo("description"))
                .body("[0].numberOfStudents", equalTo(0))
                .body("[0].uuid", notNullValue());
    }

    @Test
    void search_partialKeyword_courseReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "ourse")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("course name"));
    }

    @Test
    void search_emptyKeyword_allCoursesReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("course name"));
    }

    @Test
    void search_keywordMatchesNewlyCreatedCourse_courseReturned() {
        var token = SignUpHelper.signUpTeacher();
        createCourse(token, "Quantum Computing", "Qubits and entanglement");

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "Quantum")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("Quantum Computing"))
                .body("[0].description", equalTo("Qubits and entanglement"))
                .body("[0].numberOfStudents", equalTo(0))
                .body("[0].uuid", notNullValue());
    }

    @Test
    void search_keywordMatchesMultipleCourses_allMatchingCoursesReturned() {
        var token = SignUpHelper.signUpTeacher();
        createCourse(token, "Advanced course", "Deep dive");

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "course")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(2))
                .body("name", containsInAnyOrder("course name", "Advanced course"));
    }

    @Test
    void search_sqlInjectionStyleKeyword_emptyArrayReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "' OR '1'='1")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }

    @Test
    void search_keywordDifferentCase_emptyArrayReturned() {
        var token = SignUpHelper.signUpTeacher();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "COURSE")

                .when()
                .get("/courses/search")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("size()", equalTo(0));
    }

    private void createCourse(String token, String name, String description) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\n" +
                        "  \"name\": \"" + name + "\",\n" +
                        "  \"description\": \"" + description + "\"\n" +
                        "}")

                .when()
                .post("/courses")

                .then()
                .statusCode(HttpStatus.CREATED.value());
    }
}
