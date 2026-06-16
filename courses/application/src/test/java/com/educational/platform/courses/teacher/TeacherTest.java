package com.educational.platform.courses.teacher;

import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link Teacher} domain model.
 *
 * <p>Adding {@code @Version} to {@code Teacher} makes the version field part of the entity contract.
 * These tests pin the construction invariants at the domain level (no JPA), complementing the
 * JPA-level assertions in {@code CourseOptimisticLockingTest}.
 */
public class TeacherTest {

    @Test
    void create_validCommand_usernameSet() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher-user");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher)
                .hasFieldOrPropertyWithValue("username", "teacher-user");
    }

    @Test
    void create_validCommand_versionIsNullBeforePersist() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher-user");

        // when
        final Teacher teacher = new Teacher(command);

        // then - the @Version field is JPA-managed; before persist it must be null so that Hibernate
        // initialises it to 0 on INSERT rather than issuing an UPDATE with a stale snapshot
        assertThat(ReflectionTestUtils.getField(teacher, "version")).isNull();
    }

    @Test
    void create_validCommand_idIsNullBeforePersist() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher-user");

        // when
        final Teacher teacher = new Teacher(command);

        // then
        assertThat(teacher.getId()).isNull();
    }

    @Test
    void toIdentity_returnsUsername() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("teacher-user");
        final Teacher teacher = new Teacher(command);

        // when
        final String identity = teacher.toIdentity();

        // then
        assertThat(identity).isEqualTo("teacher-user");
    }
}
