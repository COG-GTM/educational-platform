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

	@Test
	void equals_differentUsername_areNotEqual() {
		// when / then
		assertThat(new CreateStudentCommand("alice")).isNotEqualTo(new CreateStudentCommand("bob"));
	}

	@Test
	void hashCode_sameUsername_sameHashCode() {
		// when / then
		assertThat(new CreateStudentCommand("user").hashCode())
				.isEqualTo(new CreateStudentCommand("user").hashCode());
	}

	@Test
	void constructor_whitespaceUsername_storesWhitespace() {
		// when
		final CreateStudentCommand command = new CreateStudentCommand("  ");

		// then
		assertThat(command.username()).isEqualTo("  ");
	}

	@Test
	void toString_containsUsername() {
		// when
		final CreateStudentCommand command = new CreateStudentCommand("john");

		// then
		assertThat(command.toString()).contains("john");
	}

	@Test
	void equals_null_isNotEqual() {
		// given
		final CreateStudentCommand command = new CreateStudentCommand("user");

		// then
		assertThat(command).isNotEqualTo(null);
	}
}
