package com.educational.platform.course.enrollments.student.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateStudentCommandTest {

	@Test
	void constructor_validUsername_storesUsername() {
		// when
		final CreateStudentCommand command = new CreateStudentCommand("john");

		// then
		assertThat(command.username()).isEqualTo("john");
	}

	@Test
	void constructor_nullUsername_acceptsNull() {
		// when
		final CreateStudentCommand command = new CreateStudentCommand(null);

		// then
		assertThat(command.username()).isNull();
	}

	@Test
	void constructor_emptyUsername_storesEmpty() {
		// when
		final CreateStudentCommand command = new CreateStudentCommand("");

		// then
		assertThat(command.username()).isEmpty();
	}

	@Test
	void equals_sameUsername_areEqual() {
		// when / then
		assertThat(new CreateStudentCommand("user")).isEqualTo(new CreateStudentCommand("user"));
	}
}
