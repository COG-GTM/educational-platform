package com.educational.platform.users;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleDTOTest {

    @Test
    void roleDTO_containsExpectedValues() {
        assertThat(RoleDTO.values()).containsExactly(RoleDTO.ROLE_STUDENT, RoleDTO.ROLE_TEACHER);
    }

    @Test
    void roleDTO_valueOfRoleStudent() {
        assertThat(RoleDTO.valueOf("ROLE_STUDENT")).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void roleDTO_valueOfRoleTeacher() {
        assertThat(RoleDTO.valueOf("ROLE_TEACHER")).isEqualTo(RoleDTO.ROLE_TEACHER);
    }
}
