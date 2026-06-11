package com.educational.platform.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoleTest {

    @Test
    void from_roleStudent_returnsRoleStudent() {
        // when
        final Role result = Role.from(RoleDTO.ROLE_STUDENT);

        // then
        assertThat(result).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void from_roleTeacher_returnsRoleTeacher() {
        // when
        final Role result = Role.from(RoleDTO.ROLE_TEACHER);

        // then
        assertThat(result).isEqualTo(Role.ROLE_TEACHER);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void getAuthority_allRoles_returnsName(Role role) {
        // when
        final String authority = role.getAuthority();

        // then
        assertThat(authority).isEqualTo(role.name());
    }

    @Test
    void toDTO_roleStudent_returnsRoleDTOStudent() {
        // when
        final RoleDTO result = Role.ROLE_STUDENT.toDTO();

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_roleTeacher_returnsRoleDTOTeacher() {
        // when
        final RoleDTO result = Role.ROLE_TEACHER.toDTO();

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_roleAdmin_returnsNull() {
        // when
        final RoleDTO result = Role.ROLE_ADMIN.toDTO();

        // then
        assertThat(result).isNull();
    }

    @Test
    void from_roleStudent_roundTripsCorrectly() {
        // given
        final Role original = Role.ROLE_STUDENT;

        // when
        final RoleDTO dto = original.toDTO();
        final Role roundTripped = Role.from(dto);

        // then
        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void from_roleTeacher_roundTripsCorrectly() {
        // given
        final Role original = Role.ROLE_TEACHER;

        // when
        final RoleDTO dto = original.toDTO();
        final Role roundTripped = Role.from(dto);

        // then
        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void from_null_throwsNullPointerException() {
        // when / then
        assertThatThrownBy(() -> Role.from(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(RoleDTO.class)
    void from_allRoleDTOValues_returnsNonNull(RoleDTO roleDTO) {
        // when
        final Role result = Role.from(roleDTO);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    void getAuthority_roleAdmin_returnsRoleAdmin() {
        // when
        final String authority = Role.ROLE_ADMIN.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_ADMIN");
    }
}
