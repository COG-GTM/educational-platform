package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherTest {

    @Test
    void create_validCommand_usernameStored() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("username");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher).hasFieldOrPropertyWithValue("username", "username");
        assertThat(teacher.toIdentity()).isEqualTo("username");
    }

    @Test
    void getId_freshlyCreated_isNull() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("username"));

        // then
        assertThat(teacher.getId()).isNull();
    }

    @Test
    void createTeacherCommand_exposesUsername() {
        assertThat(new CreateTeacherCommand("username").username()).isEqualTo("username");
    }
}
