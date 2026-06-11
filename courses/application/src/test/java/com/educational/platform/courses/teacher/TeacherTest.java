package com.educational.platform.courses.teacher;

import com.educational.platform.common.domain.AggregateRoot;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TeacherTest {

    @Test
    void constructor_usernameStoredFromCommand() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("john.doe");

        // when
        final Teacher sut = new Teacher(command);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("username", "john.doe");
    }

    @Test
    void toIdentity_returnsUsername() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("alice");
        final Teacher sut = new Teacher(command);

        // when
        final String identity = sut.toIdentity();

        // then
        assertThat(identity).isEqualTo("alice");
    }

    @Test
    void toIdentity_calledTwice_returnsSameValue() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("bob");
        final Teacher sut = new Teacher(command);

        // when
        final String first = sut.toIdentity();
        final String second = sut.toIdentity();

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void getId_newTeacher_returnsNull() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("user");
        final Teacher sut = new Teacher(command);

        // when
        final Integer id = sut.getId();

        // then
        assertThat(id).isNull();
    }

    @Test
    void constructor_nullUsername_storedAsNull() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand(null);

        // when
        final Teacher sut = new Teacher(command);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("username", null);
        assertThat(sut.toIdentity()).isNull();
    }

    @Test
    void constructor_emptyUsername_storedAsEmpty() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("");

        // when
        final Teacher sut = new Teacher(command);

        // then
        assertThat(sut.toIdentity()).isEmpty();
    }

    @Test
    void constructor_specialCharactersInUsername_preserved() {
        // given
        final CreateTeacherCommand command = new CreateTeacherCommand("user.name+tag@org");

        // when
        final Teacher sut = new Teacher(command);

        // then
        assertThat(sut.toIdentity()).isEqualTo("user.name+tag@org");
    }

    @Test
    void implementsAggregateRoot() {
        // then
        assertThat(AggregateRoot.class).isAssignableFrom(Teacher.class);
    }

    @Test
    void twoTeachers_differentUsernames_differentIdentities() {
        // given
        final Teacher teacher1 = new Teacher(new CreateTeacherCommand("alice"));
        final Teacher teacher2 = new Teacher(new CreateTeacherCommand("bob"));

        // then
        assertThat(teacher1.toIdentity()).isNotEqualTo(teacher2.toIdentity());
    }
}
