package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentTest {

	@Test
	void constructor_validCommand_storesUsername() {
		// given
		final CreateStudentCommand command = new CreateStudentCommand("john");

		// when
		final Student student = new Student(command);

		// then
		assertThat(student.toReference()).isEqualTo("john");
	}

	@Test
	void constructor_nullUsername_storesNull() {
		// given
		final CreateStudentCommand command = new CreateStudentCommand(null);

		// when
		final Student student = new Student(command);

		// then
		assertThat(student.toReference()).isNull();
	}

	@Test
	void constructor_emptyUsername_storesEmpty() {
		// given
		final CreateStudentCommand command = new CreateStudentCommand("");

		// when
		final Student student = new Student(command);

		// then
		assertThat(student.toReference()).isEmpty();
	}

	@Test
	void getId_beforePersist_returnsNull() {
		// given
		final Student student = new Student(new CreateStudentCommand("user"));

		// when / then
		assertThat(student.getId()).isNull();
	}
}
