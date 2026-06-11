package com.educational.platform.course.enrollments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentRequestTest {

	@Test
	void constructor_validStudent_storesStudent() {
		// when
		final CourseEnrollmentRequest request = new CourseEnrollmentRequest("username");

		// then
		assertThat(request.student()).isEqualTo("username");
	}

	@Test
	void constructor_nullStudent_acceptsNull() {
		// when
		final CourseEnrollmentRequest request = new CourseEnrollmentRequest(null);

		// then
		assertThat(request.student()).isNull();
	}

	@Test
	void constructor_emptyStudent_storesEmpty() {
		// when
		final CourseEnrollmentRequest request = new CourseEnrollmentRequest("");

		// then
		assertThat(request.student()).isEmpty();
	}

	@Test
	void equals_sameStudent_areEqual() {
		// when / then
		assertThat(new CourseEnrollmentRequest("user")).isEqualTo(new CourseEnrollmentRequest("user"));
	}

	@Test
	void equals_differentStudent_areNotEqual() {
		// when / then
		assertThat(new CourseEnrollmentRequest("alice")).isNotEqualTo(new CourseEnrollmentRequest("bob"));
	}

	@Test
	void hashCode_sameStudent_sameHashCode() {
		// when / then
		assertThat(new CourseEnrollmentRequest("user").hashCode())
				.isEqualTo(new CourseEnrollmentRequest("user").hashCode());
	}

	@Test
	void constructor_whitespaceStudent_storesWhitespace() {
		// when
		final CourseEnrollmentRequest request = new CourseEnrollmentRequest("  ");

		// then
		assertThat(request.student()).isEqualTo("  ");
	}
}
