package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentTest {

    @Test
    void create_validCommand_studentCreatedWithUsername() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("student");

        // when
        final Student student = new Student(command);

        // then
        assertThat(student)
                .hasFieldOrPropertyWithValue("username", "student");
        assertThat(student.getId()).isNull();
    }

    @Test
    void toReference_returnsUsername() {
        // given - the username is the reference carried to the courses module in the StudentEnrolledToCourse event
        final Student student = new Student(new CreateStudentCommand("student"));

        // when
        final String reference = student.toReference();

        // then
        assertThat(reference).isEqualTo("student");
    }

    @Test
    void create_emptyUsername_referenceMappedVerbatim() {
        // given - the domain performs no validation; an empty username is stored as-is
        final CreateStudentCommand command = new CreateStudentCommand("");

        // when
        final Student student = new Student(command);

        // then
        assertThat(student)
                .hasFieldOrPropertyWithValue("username", "");
        assertThat(student.toReference()).isEmpty();
    }

    @Test
    void create_nullUsername_referenceIsNull() {
        // given - a null username is forwarded verbatim, the domain does not reject it
        final CreateStudentCommand command = new CreateStudentCommand(null);

        // when
        final Student student = new Student(command);

        // then
        assertThat(student)
                .hasFieldOrPropertyWithValue("username", null);
        assertThat(student.toReference()).isNull();
    }
}
