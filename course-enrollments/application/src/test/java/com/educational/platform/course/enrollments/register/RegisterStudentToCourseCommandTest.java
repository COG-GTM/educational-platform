package com.educational.platform.course.enrollments.register;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterStudentToCourseCommandTest {

	@Test
	void constructor_validUuid_storesCourseId() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

		// then
		assertThat(command.courseId()).isEqualTo(courseId);
	}

	@Test
	void constructor_nullUuid_acceptsNull() {
		// when
		final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(null);

		// then
		assertThat(command.courseId()).isNull();
	}

	@Test
	void equals_sameUuid_areEqual() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final RegisterStudentToCourseCommand cmd1 = new RegisterStudentToCourseCommand(courseId);
		final RegisterStudentToCourseCommand cmd2 = new RegisterStudentToCourseCommand(courseId);

		// then
		assertThat(cmd1).isEqualTo(cmd2);
	}

	@Test
	void equals_differentUuid_areNotEqual() {
		// given
		final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
		final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

		// when
		final RegisterStudentToCourseCommand cmd1 = new RegisterStudentToCourseCommand(uuid1);
		final RegisterStudentToCourseCommand cmd2 = new RegisterStudentToCourseCommand(uuid2);

		// then
		assertThat(cmd1).isNotEqualTo(cmd2);
	}

	@Test
	void hashCode_sameUuid_sameHashCode() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when / then
		assertThat(new RegisterStudentToCourseCommand(courseId).hashCode())
				.isEqualTo(new RegisterStudentToCourseCommand(courseId).hashCode());
	}

	@Test
	void toString_containsCourseId() {
		// given
		final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

		// when
		final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

		// then
		assertThat(command.toString()).contains(courseId.toString());
	}

	@Test
	void equals_nullCommand_notEqual() {
		// given
		final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(UUID.randomUUID());

		// then
		assertThat(command).isNotEqualTo(null);
	}
}
