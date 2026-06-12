package com.educational.platform.users.registration;

import com.educational.platform.users.RoleDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegistrationCommandTest {

    @Test
    void builder_allFieldsSet_commandCreated() {
        // when
        final UserRegistrationCommand sut = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("student1")
                .email("student@example.com")
                .password("password123")
                .build();

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(sut.username()).isEqualTo("student1");
        assertThat(sut.email()).isEqualTo("student@example.com");
        assertThat(sut.password()).isEqualTo("password123");
    }

    @Test
    void builder_teacherRole_commandCreated() {
        // when
        final UserRegistrationCommand sut = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher1")
                .email("teacher@example.com")
                .password("password123")
                .build();

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void builder_nullFields_commandCreatedWithNulls() {
        // when
        final UserRegistrationCommand sut = UserRegistrationCommand.builder().build();

        // then
        assertThat(sut.role()).isNull();
        assertThat(sut.username()).isNull();
        assertThat(sut.email()).isNull();
        assertThat(sut.password()).isNull();
    }

    @Test
    void record_directConstructor_fieldsAccessible() {
        // when
        final UserRegistrationCommand sut = new UserRegistrationCommand(
                RoleDTO.ROLE_STUDENT, "user", "user@mail.com", "pass12345");

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(sut.username()).isEqualTo("user");
        assertThat(sut.email()).isEqualTo("user@mail.com");
        assertThat(sut.password()).isEqualTo("pass12345");
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UserRegistrationCommand cmd1 = new UserRegistrationCommand(
                RoleDTO.ROLE_STUDENT, "user", "email", "pass");
        final UserRegistrationCommand cmd2 = new UserRegistrationCommand(
                RoleDTO.ROLE_STUDENT, "user", "email", "pass");

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentUsername_notEqual() {
        // given
        final UserRegistrationCommand cmd1 = new UserRegistrationCommand(
                RoleDTO.ROLE_STUDENT, "user1", "email", "pass");
        final UserRegistrationCommand cmd2 = new UserRegistrationCommand(
                RoleDTO.ROLE_STUDENT, "user2", "email", "pass");

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
