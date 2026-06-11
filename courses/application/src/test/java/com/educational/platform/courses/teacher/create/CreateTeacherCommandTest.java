package com.educational.platform.courses.teacher.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateTeacherCommandTest {

    @Test
    void constructor_usernameStored() {
        // when
        final CreateTeacherCommand sut = new CreateTeacherCommand("john.doe");

        // then
        assertThat(sut.username()).isEqualTo("john.doe");
    }

    @Test
    void constructor_nullUsername_storedAsNull() {
        // when
        final CreateTeacherCommand sut = new CreateTeacherCommand(null);

        // then
        assertThat(sut.username()).isNull();
    }

    @Test
    void constructor_emptyUsername_storedAsEmpty() {
        // when
        final CreateTeacherCommand sut = new CreateTeacherCommand("");

        // then
        assertThat(sut.username()).isEmpty();
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final CreateTeacherCommand a = new CreateTeacherCommand("alice");
        final CreateTeacherCommand b = new CreateTeacherCommand("alice");

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentInstances_areNotEqual() {
        // given
        final CreateTeacherCommand a = new CreateTeacherCommand("alice");
        final CreateTeacherCommand b = new CreateTeacherCommand("bob");

        // then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void constructor_specialCharacters_preserved() {
        // when
        final CreateTeacherCommand sut = new CreateTeacherCommand("user+tag@example.com");

        // then
        assertThat(sut.username()).isEqualTo("user+tag@example.com");
    }
}
