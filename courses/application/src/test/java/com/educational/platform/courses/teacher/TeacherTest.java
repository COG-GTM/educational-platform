package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherTest {

    @Test
    void create_validCommand_teacherCreated() {
        // when
        final Teacher teacher = new Teacher(new CreateTeacherCommand("username"));

        // then
        assertThat(teacher.toIdentity()).isEqualTo("username");
    }

    @Test
    void getId_returnsId() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("username"));
        ReflectionTestUtils.setField(teacher, "id", 42);

        // then
        assertThat(teacher.getId()).isEqualTo(42);
    }
}
