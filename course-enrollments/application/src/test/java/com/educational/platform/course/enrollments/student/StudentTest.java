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

	@Test
	void toReference_multipleCalls_returnsSameValue() {
		// given
		final Student student = new Student(new CreateStudentCommand("john"));

		// when
		final String ref1 = student.toReference();
		final String ref2 = student.toReference();

		// then
		assertThat(ref1).isEqualTo(ref2);
	}

	@Test
	void constructor_differentUsernames_produceDifferentReferences() {
		// given
		final Student student1 = new Student(new CreateStudentCommand("alice"));
		final Student student2 = new Student(new CreateStudentCommand("bob"));

		// then
		assertThat(student1.toReference()).isNotEqualTo(student2.toReference());
	}
}
