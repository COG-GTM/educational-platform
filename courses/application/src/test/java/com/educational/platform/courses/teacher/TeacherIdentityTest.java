package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Teacher#toIdentity()} method and construction.
 */
public class TeacherIdentityTest {

    @Test
    void toIdentity_returnsUsername() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("john_doe"));

        // when / then
        assertThat(teacher.toIdentity()).isEqualTo("john_doe");
    }

    @Test
    void constructor_initialIdIsNull() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand("jane"));

        // then — before persistence, id is null
        assertThat(teacher.getId()).isNull();
    }
}
