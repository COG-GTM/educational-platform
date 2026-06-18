package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleTest {

    @Test
    void from_roleStudentDTO_mappedToRoleStudent() {
        // when
        final Role role = Role.from(RoleDTO.ROLE_STUDENT);

        // then
        assertThat(role).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void from_roleTeacherDTO_mappedToRoleTeacher() {
        // when
        final Role role = Role.from(RoleDTO.ROLE_TEACHER);

        // then
        assertThat(role).isEqualTo(Role.ROLE_TEACHER);
    }

    @Test
    void toDTO_roleStudent_mappedToRoleStudentDTO() {
        // when
        final RoleDTO dto = Role.ROLE_STUDENT.toDTO();

        // then
        assertThat(dto).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_roleTeacher_mappedToRoleTeacherDTO() {
        // when
        final RoleDTO dto = Role.ROLE_TEACHER.toDTO();

        // then
        assertThat(dto).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_roleAdmin_hasNoDtoRepresentation() {
        // given - RoleDTO only exposes the roles assignable at registration (student/teacher),
        // so an admin role has no DTO mapping and the method returns null

        // when
        final RoleDTO dto = Role.ROLE_ADMIN.toDTO();

        // then
        assertThat(dto).isNull();
    }

    @Test
    void getAuthority_returnsEnumName() {
        // then
        assertThat(Role.ROLE_ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.ROLE_STUDENT.getAuthority()).isEqualTo("ROLE_STUDENT");
        assertThat(Role.ROLE_TEACHER.getAuthority()).isEqualTo("ROLE_TEACHER");
    }
}
