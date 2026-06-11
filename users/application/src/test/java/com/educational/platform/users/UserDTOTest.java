package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserDTOTest {

    @Test
    void builder_allFieldsSet_dtoCreated() {
        // when
        final UserDTO sut = UserDTO.builder()
                .username("john")
                .email("john@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // then
        assertThat(sut.username()).isEqualTo("john");
        assertThat(sut.email()).isEqualTo("john@example.com");
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void builder_teacherRole_dtoCreated() {
        // when
        final UserDTO sut = UserDTO.builder()
                .username("teacher")
                .email("teacher@example.com")
                .role(RoleDTO.ROLE_TEACHER)
                .build();

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void builder_nullFields_dtoCreated() {
        // when
        final UserDTO sut = UserDTO.builder().build();

        // then
        assertThat(sut.username()).isNull();
        assertThat(sut.email()).isNull();
        assertThat(sut.role()).isNull();
    }

    @Test
    void record_equality_sameValues_equal() {
        // given
        final UserDTO dto1 = new UserDTO("john", "john@example.com", RoleDTO.ROLE_STUDENT);
        final UserDTO dto2 = new UserDTO("john", "john@example.com", RoleDTO.ROLE_STUDENT);

        // when / then
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    void record_equality_differentValues_notEqual() {
        // given
        final UserDTO dto1 = new UserDTO("john", "john@example.com", RoleDTO.ROLE_STUDENT);
        final UserDTO dto2 = new UserDTO("jane", "jane@example.com", RoleDTO.ROLE_TEACHER);

        // when / then
        assertThat(dto1).isNotEqualTo(dto2);
    }
}
