package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleDTOTest {

    @Test
    void values_allRolesExist() {
        // given
        final RoleDTO[] values = RoleDTO.values();

        // when / then
        assertThat(values).containsExactlyInAnyOrder(
                RoleDTO.ROLE_STUDENT,
                RoleDTO.ROLE_TEACHER
        );
    }

    @Test
    void valueOf_roleStudent_correctValue() {
        // when
        final RoleDTO role = RoleDTO.valueOf("ROLE_STUDENT");

        // then
        assertThat(role).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void valueOf_roleTeacher_correctValue() {
        // when
        final RoleDTO role = RoleDTO.valueOf("ROLE_TEACHER");

        // then
        assertThat(role).isEqualTo(RoleDTO.ROLE_TEACHER);
    }
}
