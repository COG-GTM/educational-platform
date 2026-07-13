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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Represents API tests covering the listing and conflict handling of course proposals.
 */
@Sql(scripts = "classpath:insert_data.sql")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "com.educational.platform.security.enabled=false")
public class CourseProposalConflictApiTest {

    private static final String EXISTING_UUID = "123e4567-e89b-12d3-a456-426655440001";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    void courseProposals_seededProposal_returnsProposals() {
        given()
                .contentType(ContentType.JSON)

                .when()
                .get("/administration/course-proposals")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(1))
                .body("[0].uuid", equalTo(EXISTING_UUID))
                .body("[0].status", equalTo("WAITING_FOR_APPROVAL"));
    }

    @Test
    void approve_alreadyApprovedCourseProposal_conflict() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", EXISTING_UUID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)

                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", EXISTING_UUID)

                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("errors", contains(containsString("already approved")));
    }

    @Test
    void decline_alreadyDeclinedCourseProposal_conflict() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", EXISTING_UUID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)

                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", EXISTING_UUID)

                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("errors", contains(containsString("already declined")));
    }

    @Test
    void approve_unknownCourseProposal_notFound() {
        given()
                .contentType(ContentType.JSON)

                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", UUID.randomUUID().toString())

                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
