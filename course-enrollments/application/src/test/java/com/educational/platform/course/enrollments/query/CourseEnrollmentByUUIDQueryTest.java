package com.educational.platform.course.enrollments.query;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseEnrollmentByUUIDQueryTest {

	@Test
	void constructor_validUuid_storesUuid() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(uuid);

		// then
		assertThat(query.uuid()).isEqualTo(uuid);
	}

	@Test
	void constructor_nullUuid_acceptsNull() {
		// when
		final CourseEnrollmentByUUIDQuery query = new CourseEnrollmentByUUIDQuery(null);

		// then
		assertThat(query.uuid()).isNull();
	}

	@Test
	void equals_sameUuid_areEqual() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when / then
		assertThat(new CourseEnrollmentByUUIDQuery(uuid)).isEqualTo(new CourseEnrollmentByUUIDQuery(uuid));
	}
}
