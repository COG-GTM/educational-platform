package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleTest {

    @Test
    void from_roleStudent_roleStudentReturned() {
        // when
        final Role result = Role.from(RoleDTO.ROLE_STUDENT);

        // then
        assertThat(result).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void from_roleTeacher_roleTeacherReturned() {
        // when
        final Role result = Role.from(RoleDTO.ROLE_TEACHER);

        // then
        assertThat(result).isEqualTo(Role.ROLE_TEACHER);
    }

    @Test
    void getAuthority_roleStudent_nameReturned() {
        // when
        final String result = Role.ROLE_STUDENT.getAuthority();

        // then
        assertThat(result).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void getAuthority_roleTeacher_nameReturned() {
        // when
        final String result = Role.ROLE_TEACHER.getAuthority();

        // then
        assertThat(result).isEqualTo("ROLE_TEACHER");
    }

    @Test
    void getAuthority_roleAdmin_nameReturned() {
        // when
        final String result = Role.ROLE_ADMIN.getAuthority();

        // then
        assertThat(result).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void toDTO_roleStudent_roleDTOStudentReturned() {
        // when
        final RoleDTO result = Role.ROLE_STUDENT.toDTO();

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_roleTeacher_roleDTOTeacherReturned() {
        // when
        final RoleDTO result = Role.ROLE_TEACHER.toDTO();

        // then
        assertThat(result).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toDTO_roleAdmin_nullReturned() {
        // when
        final RoleDTO result = Role.ROLE_ADMIN.toDTO();

        // then
        assertThat(result).isNull();
    }
}
