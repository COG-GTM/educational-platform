package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleEdgeCaseTest {

    @Test
    void from_studentRoleDTO_returnsStudentRole() {
        // when
        final Role result = Role.from(RoleDTO.ROLE_STUDENT);

        // then
        assertThat(result).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void from_teacherRoleDTO_returnsTeacherRole() {
        // when
        final Role result = Role.from(RoleDTO.ROLE_TEACHER);

        // then
        assertThat(result).isEqualTo(Role.ROLE_TEACHER);
    }

    @Test
    void getAuthority_studentRole_returnsRoleStudentString() {
        // when
        final String authority = Role.ROLE_STUDENT.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void getAuthority_teacherRole_returnsRoleTeacherString() {
        // when
        final String authority = Role.ROLE_TEACHER.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_TEACHER");
    }

    @Test
    void getAuthority_adminRole_returnsRoleAdminString() {
        // when
        final String authority = Role.ROLE_ADMIN.getAuthority();

        // then
        assertThat(authority).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void toDTO_studentRole_returnsStudentRoleDTO() {
        // when
        final RoleDTO result = Role.ROLE_STUDENT.toDTO();

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherRole_returnsTeacherRoleDTO() {
        // when
        final RoleDTO result = Role.ROLE_TEACHER.toDTO();

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_adminRole_returnsNull() {
        // when
        final RoleDTO result = Role.ROLE_ADMIN.toDTO();

        // then
        assertThat(result).isNull();
    }

    @Test
    void values_allRolesExist() {
        // when
        final Role[] values = Role.values();

        // then
        assertThat(values).containsExactlyInAnyOrder(
                Role.ROLE_ADMIN,
                Role.ROLE_STUDENT,
                Role.ROLE_TEACHER
        );
    }
}
