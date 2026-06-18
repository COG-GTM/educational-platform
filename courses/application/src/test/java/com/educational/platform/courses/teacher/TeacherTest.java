package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherTest {

    @Test
    void create_validCommand_teacherCreatedWithUsername() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("newteacher");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher)
                .hasFieldOrPropertyWithValue("username", "newteacher");
        assertThat(teacher.getId()).isNull();
    }

    @Test
    void toIdentity_returnsUsername() {
        // given - the username is the natural key used for cross-context identification
        final Teacher teacher = new Teacher(new CreateTeacherCommand("newteacher"));

        // when
        final String identity = teacher.toIdentity();

        // then
        assertThat(identity).isEqualTo("newteacher");
    }

    @Test
    void create_emptyUsername_usernameMappedVerbatim() {
        // given - the domain performs no validation; an empty username is stored as-is
        final CreateTeacherCommand command = new CreateTeacherCommand("");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher)
                .hasFieldOrPropertyWithValue("username", "");
        assertThat(teacher.toIdentity()).isEmpty();
    }

    @Test
    void create_nullUsername_usernameIsNull() {
        // given - a null username is forwarded verbatim, the domain does not reject it
        final CreateTeacherCommand command = new CreateTeacherCommand(null);

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher)
                .hasFieldOrPropertyWithValue("username", null);
        assertThat(teacher.toIdentity()).isNull();
    }
}
