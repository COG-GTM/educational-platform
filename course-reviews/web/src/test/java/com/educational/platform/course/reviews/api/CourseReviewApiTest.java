package com.educational.platform.course.reviews.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.security.SignUpHelper;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;

/**
 * Represents API tests for course reviews functionality.
 */
@Sql(scripts = "classpath:insert_data.sql")
@TestPropertySource("classpath:application-security.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CourseReviewApiTest {

	@LocalServerPort
	private int port;

	@BeforeEach
	void setup() {
		RestAssured.defaultParser = Parser.JSON;
		RestAssured.port = port;
		RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
	}

	@Test
	void reviews_validRequest_reviews() {
		var token = SignUpHelper.signUpStudent();

		final UUID reviewUuid = UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))
				.path("uuid"));

		given()
				.header("Authorization", "Bearer " + token)

				.when()
				.get("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.body("[0].course", equalTo("123e4567-e89b-12d3-a456-426655440001"))
				.body("[0].uuid", equalTo(reviewUuid.toString()))
				.body("[0].username", equalTo("username"))
				.statusCode(HttpStatus.OK.value());
	}

	@Test
	void review_validRequest_created() {
		var token = SignUpHelper.signUpStudent();

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"reviewer\": \"username\",\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.CREATED.value());
	}

	@Test
	void update_validRequest_noContent() {
		var token = SignUpHelper.signUpStudent();

		final UUID reviewUuid = UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))
				.path("uuid"));

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"comment\": \"comment2\",\n" + "  \"rating\": 3.5\n" + "}")

				.when()
				.put("/courses/{courseUuid}/reviews/{reviewUuid}", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), reviewUuid)

				.then()
				.statusCode(HttpStatus.NO_CONTENT.value());
	}

	@Test
	void updateTwice_validRequests_latestReviewPersisted() {
		// given - a review created through the API. With the @Version this PR added, the row is persisted at
		// version 0 and every subsequent update reads the current version, bumps it and writes it back.
		var token = SignUpHelper.signUpStudent();
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID reviewUuid = UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", courseUuid)
				.path("uuid"));

		// when - the same review is updated twice in a row through the full HTTP stack (controller, security filter,
		// command handler, JPA). Both updates must succeed end-to-end: the second one operates on the version-1 row
		// the first one produced, so a mishandled optimistic-lock version would fail it instead of returning 204.
		updateReview(token, courseUuid, reviewUuid, 3.5, "first update");
		updateReview(token, courseUuid, reviewUuid, 4.5, "second update");

		// then - the latest write wins and is observable through the public read API. Existing API coverage only
		// creates-then-updates once and asserts the 204 status; it never re-updates nor reads the value back, so
		// repeated version-incrementing updates through the controller were previously unverified end-to-end.
		given()
				.header("Authorization", "Bearer " + token)

				.when()
				.get("/courses/{uuid}/reviews", courseUuid)

				.then()
				.body("[0].uuid", equalTo(reviewUuid.toString()))
				.body("[0].comment", equalTo("second update"))
				.statusCode(HttpStatus.OK.value());
	}

	@Test
	void review_response_doesNotExposeInternalVersionField() {
		// given - the @Version field this PR added to the entities is an internal optimistic-lock column, not part
		// of the public API contract: neither CourseReviewCreatedResponse nor CourseReviewDTO declares it
		var token = SignUpHelper.signUpStudent();
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when - a review is created through the full HTTP stack
		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", courseUuid)

				.then()
				.statusCode(HttpStatus.CREATED.value())
				// the create response carries only the new review's uuid, never the internal version
				.body("uuid", notNullValue())
				.body("$", not(hasKey("version")));

		// then - the list payload exposes only the public CourseReviewDTO fields and must not leak the internal
		// version column. Every other version test asserts the field via reflection at the persistence layer; this
		// pins that the new concurrency-control field stays out of the public JSON contract.
		given()
				.header("Authorization", "Bearer " + token)

				.when()
				.get("/courses/{uuid}/reviews", courseUuid)

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("[0]", hasKey("uuid"))
				.body("[0]", not(hasKey("version")));
	}

	@Test
	void review_requestBodyWithUnknownVersion_ignoredAndReviewCreated() {
		// given - the @Version this PR added is an internal optimistic-lock column, not part of the public write
		// contract: ReviewCourseRequest declares only rating/comment, so a client-supplied version must not be a
		// way to seed or tamper with the optimistic-lock version
		var token = SignUpHelper.signUpStudent();
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when - a create request carries an out-of-contract version field through the full HTTP stack
		final UUID reviewUuid = UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2,\n" + "  \"version\": 99\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", courseUuid)

				.then()
				// then - the unknown version is ignored: the review is still created and the response carries only
				// the new uuid, never the internal version. review_response_doesNotExposeInternalVersionField pins
				// the output side of this contract; this pins the input side - the field is not writable via the API.
				.statusCode(HttpStatus.CREATED.value())
				.body("uuid", notNullValue())
				.body("$", not(hasKey("version")))
				.extract().path("uuid"));

		// and - the created review is readable through the public read API and still leaks no version
		given()
				.header("Authorization", "Bearer " + token)

				.when()
				.get("/courses/{uuid}/reviews", courseUuid)

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("[0].uuid", equalTo(reviewUuid.toString()))
				.body("[0]", not(hasKey("version")));
	}

	@Test
	void update_requestBodyWithUnknownVersion_ignoredAndUpdateApplied() {
		// given - a review created through the API at version 0
		var token = SignUpHelper.signUpStudent();
		final UUID courseUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID reviewUuid = UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", courseUuid)
				.path("uuid"));

		// when - an update request carries an out-of-contract version field (a client trying to drive the
		// optimistic-lock version itself). UpdateCourseReviewRequest declares only rating/comment.
		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"comment\": \"updated\",\n" + "  \"rating\": 3.5,\n" + "  \"version\": 99\n" + "}")

				.when()
				.put("/courses/{courseUuid}/reviews/{reviewUuid}", courseUuid, reviewUuid)

				.then()
				// then - the unknown version is ignored: the update still succeeds (the version is managed by
				// Hibernate from the persisted row, not the request body), rather than being rejected or used
				.statusCode(HttpStatus.NO_CONTENT.value());

		// and - the update was applied: the new comment is observable through the public read API
		given()
				.header("Authorization", "Bearer " + token)

				.when()
				.get("/courses/{uuid}/reviews", courseUuid)

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("[0].uuid", equalTo(reviewUuid.toString()))
				.body("[0].comment", equalTo("updated"))
				.body("[0]", not(hasKey("version")));
	}

	private void updateReview(String token, UUID courseUuid, UUID reviewUuid, double rating, String comment) {
		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"comment\": \"" + comment + "\",\n" + "  \"rating\": " + rating + "\n}")

				.when()
				.put("/courses/{courseUuid}/reviews/{reviewUuid}", courseUuid, reviewUuid)

				.then()
				.statusCode(HttpStatus.NO_CONTENT.value());
	}

}
