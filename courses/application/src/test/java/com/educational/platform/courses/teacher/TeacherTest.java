package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherTest {

    @Test
    void constructor_validCommand_setsUsername() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("john_doe");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher.toIdentity()).isEqualTo("john_doe");
    }

    @Test
    void constructor_newTeacher_idIsNull() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher1");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher.getId()).isNull();
    }

    @Test
    void toIdentity_returnsUsername() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("alice"));

        // then
        assertThat(teacher.toIdentity()).isEqualTo("alice");
    }
}
