package com.educational.platform.course.enrollments.course.create;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCourseCommandTest {

	@Test
	void constructor_validUuid_storesUuid() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final CreateCourseCommand command = new CreateCourseCommand(uuid);

		// then
		assertThat(command.uuid()).isEqualTo(uuid);
	}

	@Test
	void constructor_nullUuid_acceptsNull() {
		// when
		final CreateCourseCommand command = new CreateCourseCommand(null);

		// then
		assertThat(command.uuid()).isNull();
	}

	@Test
	void equals_sameUuid_areEqual() {
		// given
		final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

		// when / then
		assertThat(new CreateCourseCommand(uuid)).isEqualTo(new CreateCourseCommand(uuid));
	}
}
