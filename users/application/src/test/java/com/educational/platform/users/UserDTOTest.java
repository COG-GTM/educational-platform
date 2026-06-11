package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDTOTest {

    @Test
    void builder_allFieldsSet_dtoCreatedCorrectly() {
        // when
        final UserDTO dto = UserDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // then
        assertThat(dto.username()).isEqualTo("testuser");
        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void builder_teacherRole_dtoCreatedCorrectly() {
        // when
        final UserDTO dto = UserDTO.builder()
                .username("teacher")
                .email("teacher@example.com")
                .role(RoleDTO.ROLE_TEACHER)
                .build();

        // then
        assertThat(dto.username()).isEqualTo("teacher");
        assertThat(dto.email()).isEqualTo("teacher@example.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void builder_nullFields_dtoCreatedWithNulls() {
        // when
        final UserDTO dto = UserDTO.builder()
                .username(null)
                .email(null)
                .role(null)
                .build();

        // then
        assertThat(dto.username()).isNull();
        assertThat(dto.email()).isNull();
        assertThat(dto.role()).isNull();
    }

    @Test
    void equality_sameFielValues_equal() {
        // given
        final UserDTO dto1 = UserDTO.builder()
                .username("user")
                .email("user@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final UserDTO dto2 = UserDTO.builder()
                .username("user")
                .email("user@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // then
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    void equality_differentFieldValues_notEqual() {
        // given
        final UserDTO dto1 = UserDTO.builder()
                .username("user1")
                .email("user1@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final UserDTO dto2 = UserDTO.builder()
                .username("user2")
                .email("user2@example.com")
                .role(RoleDTO.ROLE_TEACHER)
                .build();

        // then
        assertThat(dto1).isNotEqualTo(dto2);
    }
}
