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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

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
    void decline_existingCourseProposal_noContent() {
        given()
                .contentType(ContentType.JSON)

                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void approve_courseProposalApprovedTwice_secondRequestConflict() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // the seeded row carries no version, so the first approve relies on the @Column DB default
        // to load version 0, mutate, and persist version 1 against the Hibernate-managed schema
        given()
                .contentType(ContentType.JSON)

                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", uuid.toString())

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // re-approving reloads the now-versioned (and APPROVED) aggregate and the domain guard maps to 409,
        // proving the new @Version column does not break the existing conflict contract end to end
        given()
                .contentType(ContentType.JSON)

                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", uuid.toString())

                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void decline_courseProposalDeclinedTwice_secondRequestConflict() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // the seeded row carries no version, so the first decline relies on the @Column DB default
        // to load version 0, mutate, and persist version 1 against the Hibernate-managed schema
        given()
                .contentType(ContentType.JSON)

                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", uuid.toString())

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // re-declining reloads the now-versioned (and DECLINED) aggregate and the domain guard maps to 409,
        // proving the new @Version column does not break the existing conflict contract end to end
        given()
                .contentType(ContentType.JSON)

                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", uuid.toString())

                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void approve_thenDecline_existingCourseProposal_bothNoContent() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // the seeded row starts at version 0; approving loads it, mutates and persists version 1
        given()
                .contentType(ContentType.JSON)

                .when()
                .put("/administration/course-proposals/{uuid}/approval-status", uuid.toString())

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // declining the now-APPROVED (version 1) aggregate is a legitimate cross-operation transition:
        // it reloads the versioned row, passes the optimistic-lock check and persists version 2, so the
        // new @Version column must not block valid sequential edits across different operations
        given()
                .contentType(ContentType.JSON)

                .when()
                .delete("/administration/course-proposals/{uuid}/approval-status", uuid.toString())

                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void courseProposals_listing_doesNotExposeVersionField() {
        // the new @Version column is a persistence concern; the read API contract (CourseProposalDTO)
        // must keep exposing only uuid + status and must not leak the version field to API clients
        given()
                .contentType(ContentType.JSON)

                .when()
                .get("/administration/course-proposals")

                .then()
                .statusCode(HttpStatus.OK.value())
                .body("uuid", hasItem("123e4567-e89b-12d3-a456-426655440001"))
                .body("status", hasItem("WAITING_FOR_APPROVAL"))
                .body("$", everyItem(not(hasKey("version"))));
    }

}
