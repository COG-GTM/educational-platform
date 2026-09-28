package com.educational.platform.course.reviews.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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
	void review_alreadyReviewed_unprocessableEntity() {
		var token = SignUpHelper.signUpStudent();

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.CREATED.value());

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 4.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
	}

	@Test
	void review_notEnrolledCourse_forbidden() {
		var token = SignUpHelper.signUpStudent();

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440002"))

				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
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
	void review_userIsTeacher_forbidden() {
		var token = signUp("ROLE_TEACHER", "teacher-username", "teacher@gmail.com");

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + token)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))

				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	@Test
	void update_anotherUsersReview_forbidden() {
		var reviewerToken = SignUpHelper.signUpStudent();

		final UUID reviewUuid = UUID.fromString(given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + reviewerToken)
				.body("{\n" + "  \"rating\": 3.2\n" + "}")

				.when()
				.post("/courses/{uuid}/reviews", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"))
				.path("uuid"));

		var anotherStudentToken = signUp("ROLE_STUDENT", "another-username", "another@gmail.com");

		given()
				.contentType(ContentType.JSON)
				.header("Authorization", "Bearer " + anotherStudentToken)
				.body("{\n" + "  \"comment\": \"comment2\",\n" + "  \"rating\": 3.5\n" + "}")

				.when()
				.put("/courses/{courseUuid}/reviews/{reviewUuid}", UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), reviewUuid)

				.then()
				.statusCode(HttpStatus.FORBIDDEN.value());
	}

	private static String signUp(String role, String username, String email) {
		var response = given()
				.contentType(ContentType.JSON)
				.body("{\n" + "  \"role\": \"" + role + "\",\n" + "  \"username\": \"" + username + "\",\n"
						+ "  \"email\": \"" + email + "\",\n" + "  \"password\": \"password\"\n" + "}")

				.when()
				.post("/users/sign-up")
				.then()
				.extract()
				.response();

		if (response.statusCode() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
			return given()
					.contentType(ContentType.JSON)
					.body("{\n" + "  \"username\": \"" + username + "\",\n" + "  \"password\": \"password\"\n" + "}")

					.when()
					.post("/users/sign-in")
					.asString();
		}

		return response.body().asString();
	}

}
