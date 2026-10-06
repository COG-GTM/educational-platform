package com.educational.platform.course.reviews.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
	void reviews_anotherCourseReviewed_onlyRequestedCourseReviewsReturnedMostRecentFirst() {
		var token = SignUpHelper.signUpStudent();

		final UUID firstReviewUuid = review(token, "123e4567-e89b-12d3-a456-426655440001", 2.0, "first");
		final UUID secondReviewUuid = review(token, "123e4567-e89b-12d3-a456-426655440001", 5.0, "second");
		review(token, "123e4567-e89b-12d3-a456-426655440002", 1.0, "other course");

		given()
				.when()
				.get("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("$", hasSize(2))
				.body("[0].uuid", equalTo(secondReviewUuid.toString()))
				.body("[0].comment", equalTo("second"))
				.body("[0].createdDate", notNullValue())
				.body("[1].uuid", equalTo(firstReviewUuid.toString()));
	}

	@Test
	void summary_anonymousRequest_breakdownReturned() {
		var token = SignUpHelper.signUpStudent();

		review(token, "123e4567-e89b-12d3-a456-426655440001", 4.0, "good");
		review(token, "123e4567-e89b-12d3-a456-426655440001", 4.4, "good too");
		review(token, "123e4567-e89b-12d3-a456-426655440001", 1.0, "bad");
		review(token, "123e4567-e89b-12d3-a456-426655440002", 5.0, "other course");

		given()
				.when()
				.get("/courses/{uuid}/reviews/summary", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("totalReviews", equalTo(3))
				.body("averageRating", equalTo(3.1333334f))
				.body("ratingCounts.1", equalTo(1))
				.body("ratingCounts.2", equalTo(0))
				.body("ratingCounts.3", equalTo(0))
				.body("ratingCounts.4", equalTo(2))
				.body("ratingCounts.5", equalTo(0));
	}

	@Test
	void summary_noReviews_zeroes() {
		given()
				.when()
				.get("/courses/{uuid}/reviews/summary", UUID.randomUUID())

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("totalReviews", equalTo(0))
				.body("averageRating", equalTo(0.0f))
				.body("ratingCounts.5", equalTo(0));
	}

	private UUID review(String token, String courseUuid, double rating, String comment) {
		return UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": " + rating + ",\n" + "  \"comment\": \"" + comment + "\"\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString(courseUuid))
				.path("uuid"));
	}

	@Test
	void review_anonymousRequest_forbidden() {
		given()
				.contentType(ContentType.JSON)
				.body("{\n" + "  \"rating\": 3.2,\n" + "  \"comment\": \"comment\"\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());

		given()
				.when()
				.get("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.OK.value())
				.body("$", hasSize(0));
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

}
