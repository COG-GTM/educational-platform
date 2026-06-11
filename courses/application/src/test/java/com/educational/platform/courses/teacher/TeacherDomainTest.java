package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherDomainTest {

    @Test
    void constructor_setsUsername() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher1");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher.toIdentity()).isEqualTo("teacher1");
    }

    @Test
    void toIdentity_returnsUsername() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("jane.doe"));

        // then
        assertThat(teacher.toIdentity()).isEqualTo("jane.doe");
    }

    @Test
    void getId_newTeacher_returnsNull() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("user"));

        // then
        assertThat(teacher.getId()).isNull();
    }

    @Test
    void createTeacherCommand_recordAccessor() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("admin");

        // then
        assertThat(command.username()).isEqualTo("admin");
    }

    @Test
    void createTeacherCommand_equalInstances() {
        assertThat(new CreateTeacherCommand("user"))
                .isEqualTo(new CreateTeacherCommand("user"));
    }

    @Test
    void createTeacherCommand_differentUsernames_notEqual() {
        assertThat(new CreateTeacherCommand("user1"))
                .isNotEqualTo(new CreateTeacherCommand("user2"));
    }
}
