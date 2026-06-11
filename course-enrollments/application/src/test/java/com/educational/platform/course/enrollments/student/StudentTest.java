package com.educational.platform.course.enrollments.student;

import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentTest {

    @Test
    void create_validCommand_usernameStored() {
        // given
        final CreateStudentCommand command = new CreateStudentCommand("username");

        // when
        final Student student = new Student(command);

        // then
        assertThat(student).hasFieldOrPropertyWithValue("username", "username");
        assertThat(student.toReference()).isEqualTo("username");
    }

    @Test
    void getId_freshlyCreated_isNull() {
        final Student student = new Student(new CreateStudentCommand("username"));
        assertThat(student.getId()).isNull();
    }

    @Test
    void createStudentCommand_exposesUsername() {
        assertThat(new CreateStudentCommand("username").username()).isEqualTo("username");
    }
}
