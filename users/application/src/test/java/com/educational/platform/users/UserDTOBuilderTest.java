package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDTOBuilderTest {

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
                .username("teacher1")
                .email("teacher@example.com")
                .role(RoleDTO.ROLE_TEACHER)
                .build();

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void builder_nullFields_dtoCreatedWithNulls() {
        // when
        final UserDTO sut = UserDTO.builder().build();

        // then
        assertThat(sut.username()).isNull();
        assertThat(sut.email()).isNull();
        assertThat(sut.role()).isNull();
    }

    @Test
    void record_directConstructor_fieldsAccessible() {
        // when
        final UserDTO sut = new UserDTO("user", "user@mail.com", RoleDTO.ROLE_STUDENT);

        // then
        assertThat(sut.username()).isEqualTo("user");
        assertThat(sut.email()).isEqualTo("user@mail.com");
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UserDTO dto1 = new UserDTO("user", "email", RoleDTO.ROLE_STUDENT);
        final UserDTO dto2 = new UserDTO("user", "email", RoleDTO.ROLE_STUDENT);

        // then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final UserDTO dto1 = new UserDTO("user1", "email", RoleDTO.ROLE_STUDENT);
        final UserDTO dto2 = new UserDTO("user2", "email", RoleDTO.ROLE_STUDENT);

        // then
        assertThat(dto1).isNotEqualTo(dto2);
    }
}
