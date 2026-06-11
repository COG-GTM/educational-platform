package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleTest {

    @Test
    void from_studentDTO_mapsToStudentRole() {
        assertThat(Role.from(RoleDTO.ROLE_STUDENT)).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void from_teacherDTO_mapsToTeacherRole() {
        assertThat(Role.from(RoleDTO.ROLE_TEACHER)).isEqualTo(Role.ROLE_TEACHER);
    }

    @Test
    void toDTO_studentRole_mapsToStudentDTO() {
        assertThat(Role.ROLE_STUDENT.toDTO()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherRole_mapsToTeacherDTO() {
        assertThat(Role.ROLE_TEACHER.toDTO()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_adminRole_returnsNull() {
        assertThat(Role.ROLE_ADMIN.toDTO()).isNull();
    }

    @Test
    void getAuthority_returnsName() {
        assertThat(Role.ROLE_ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
        assertThat(Role.ROLE_STUDENT.getAuthority()).isEqualTo("ROLE_STUDENT");
        assertThat(Role.ROLE_TEACHER.getAuthority()).isEqualTo("ROLE_TEACHER");
    }
}
