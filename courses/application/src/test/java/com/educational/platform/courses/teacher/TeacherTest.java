package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherTest {

    @Test
    void create_validCommand_teacherCreated() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher-username");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher)
                .hasFieldOrPropertyWithValue("username", "teacher-username");
    }

    @Test
    void toIdentity_createdTeacher_usernameReturned() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher-username");
        final Teacher teacher = new Teacher(command);

        // when
        final String identity = teacher.toIdentity();

        // then
        assertThat(identity).isEqualTo("teacher-username");
    }
}
