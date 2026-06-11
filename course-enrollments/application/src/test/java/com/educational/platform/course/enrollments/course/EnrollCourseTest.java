package com.educational.platform.course.enrollments.course;

import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollCourseTest {

	@Test
	void constructor_validCommand_storesUuid() {
		// given
		final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final CreateCourseCommand command = new CreateCourseCommand(uuid);

		// when
		final EnrollCourse course = new EnrollCourse(command);

		// then
		assertThat(course.toReference()).isEqualTo(uuid);
	}

	@Test
	void constructor_nullUuid_storesNull() {
		// given
		final CreateCourseCommand command = new CreateCourseCommand(null);

		// when
		final EnrollCourse course = new EnrollCourse(command);

		// then
		assertThat(course.toReference()).isNull();
	}

	@Test
	void getId_beforePersist_returnsNull() {
		// given
		final CreateCourseCommand command = new CreateCourseCommand(UUID.randomUUID());

		// when
		final EnrollCourse course = new EnrollCourse(command);

		// then
		assertThat(course.getId()).isNull();
	}

	@Test
	void toReference_returnsUuidFromCommand() {
		// given
		final UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(uuid));

		// when
		final UUID reference = course.toReference();

		// then
		assertThat(reference).isEqualTo(uuid);
	}
}
