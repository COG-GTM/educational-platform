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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

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
    void catalog_anonymousRequest_publishedCoursesReturned() {
        given()
                .when()
                .get("/courses?search=published&sort=RATING")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("totalElements", equalTo(1))
                .body("items[0].name", equalTo("published course"))
                .body("items[0].teacherName", equalTo("username"))
                .body("items[0].rating", equalTo(4.5f))
                .body("items[0].numberOfStudents", equalTo(3));
    }

    @Test
    void catalogFacets_anonymousRequest_facetsReturned() {
        given()
                .when()
                .get("/courses/catalog-facets")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("categories", hasItem("Programming"))
                .body("teachers", hasItem("username"));
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
}
