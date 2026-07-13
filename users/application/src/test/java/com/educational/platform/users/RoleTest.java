package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void from_student_mapsToStudentRole() {
        assertThat(Role.from(RoleDTO.ROLE_STUDENT)).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void from_teacher_mapsToTeacherRole() {
        assertThat(Role.from(RoleDTO.ROLE_TEACHER)).isEqualTo(Role.ROLE_TEACHER);
    }

    @Test
    void getAuthority_returnsEnumName() {
        assertThat(Role.ROLE_ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.ROLE_STUDENT.getAuthority()).isEqualTo("ROLE_STUDENT");
        assertThat(Role.ROLE_TEACHER.getAuthority()).isEqualTo("ROLE_TEACHER");
    }

    @Test
    void toDTO_studentAndTeacher_mapToDto() {
        assertThat(Role.ROLE_STUDENT.toDTO()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(Role.ROLE_TEACHER.toDTO()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_admin_hasNoDtoRepresentation() {
        assertThat(Role.ROLE_ADMIN.toDTO()).isNull();
    }
}
