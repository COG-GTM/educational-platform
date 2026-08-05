package com.educational.platform.administration.course.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Represents API tests for administration of course proposal functionality.
 */
@Sql(scripts = "classpath:insert_data.sql")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "com.educational.platform.security.enabled=false")
public class CourseProposalApiTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void approve_existingCourseProposal_noContent() {
        given()
                .contentType(ContentType.JSON)

                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", UUID.fromString("123e4567-e89b-12d3-a456-426655440001").toString())

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void courseProposals_paged_ok() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 10)

                .when()
                .get("/administration/course-proposals")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", equalTo(1))
                .body("content[0].uuid", equalTo("123e4567-e89b-12d3-a456-426655440001"))
                .body("page.size", equalTo(10));
    }

    @Test
    void decline_existingCourseProposal_noContent() {
        given()
                .contentType(ContentType.JSON)

                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

}
