package com.educational.platform.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoleDTOTest {

    @Test
    void values_returnsBothRoles() {
        // when
        final RoleDTO[] values = RoleDTO.values();

        // then
        assertThat(values).hasSize(2);
        assertThat(values).containsExactly(RoleDTO.ROLE_STUDENT, RoleDTO.ROLE_TEACHER);
    }

    @Test
    void valueOf_roleStudent_returnsCorrectConstant() {
        // when
        final RoleDTO result = RoleDTO.valueOf("ROLE_STUDENT");

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void valueOf_roleTeacher_returnsCorrectConstant() {
        // when
        final RoleDTO result = RoleDTO.valueOf("ROLE_TEACHER");

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void valueOf_invalidName_throwsIllegalArgumentException() {
        // when / then
        assertThatThrownBy(() -> RoleDTO.valueOf("ROLE_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void valueOf_null_throwsNullPointerException() {
        // when / then
        assertThatThrownBy(() -> RoleDTO.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EnumSource(RoleDTO.class)
    void name_allValues_returnsNonBlankName(RoleDTO role) {
        // when
        final String name = role.name();

        // then
        assertThat(name).isNotBlank();
        assertThat(name).startsWith("ROLE_");
    }

    @Test
    void ordinal_roleStudent_isZero() {
        // then
        assertThat(RoleDTO.ROLE_STUDENT.ordinal()).isZero();
    }

    @Test
    void ordinal_roleTeacher_isOne() {
        // then
        assertThat(RoleDTO.ROLE_TEACHER.ordinal()).isEqualTo(1);
    }

    @Test
    void from_roleStudent_roundTripsViaRoleEnum() {
        // given
        final Role role = Role.from(RoleDTO.ROLE_STUDENT);

        // when
        final RoleDTO dto = role.toDTO();

        // then
        assertThat(dto).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void from_roleTeacher_roundTripsViaRoleEnum() {
        // given
        final Role role = Role.from(RoleDTO.ROLE_TEACHER);

        // when
        final RoleDTO dto = role.toDTO();

        // then
        assertThat(dto).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toString_roleStudent_returnsName() {
        // when
        final String str = RoleDTO.ROLE_STUDENT.toString();

        // then
        assertThat(str).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void toString_roleTeacher_returnsName() {
        // when
        final String str = RoleDTO.ROLE_TEACHER.toString();

        // then
        assertThat(str).isEqualTo("ROLE_TEACHER");
    }
}
