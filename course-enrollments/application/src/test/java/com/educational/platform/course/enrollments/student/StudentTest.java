package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentTest {

    @Test
    void constructor_validCommand_studentCreated() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("username");

        // when
        final Student sut = new Student(command);

        // then
        assertThat(sut.toReference()).isEqualTo("username");
    }

    @Test
    void toReference_returnsUsername() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("john_doe");
        final Student sut = new Student(command);

        // when
        final String reference = sut.toReference();

        // then
        assertThat(reference).isEqualTo("john_doe");
    }
}
