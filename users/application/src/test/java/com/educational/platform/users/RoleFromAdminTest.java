package com.educational.platform.users;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests edge cases for {@link Role#from(RoleDTO)} conversion
 * and verifies the {@link GrantedAuthority} contract.
 */
public class RoleFromAdminTest {

    @Test
    void role_implementsGrantedAuthority() {
        // then
        assertThat(Role.ROLE_ADMIN).isInstanceOf(GrantedAuthority.class);
        assertThat(Role.ROLE_STUDENT).isInstanceOf(GrantedAuthority.class);
        assertThat(Role.ROLE_TEACHER).isInstanceOf(GrantedAuthority.class);
    }

    @Test
    void from_studentDTO_roundTripsCorrectly() {
        // given
        final Role role = Role.from(RoleDTO.ROLE_STUDENT);

        // when
        final RoleDTO dto = role.toDTO();

        // then
        assertThat(dto).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void from_teacherDTO_roundTripsCorrectly() {
        // given
        final Role role = Role.from(RoleDTO.ROLE_TEACHER);

        // when
        final RoleDTO dto = role.toDTO();

        // then
        assertThat(dto).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void allRoles_haveDistinctAuthorities() {
        // when
        final String adminAuth = Role.ROLE_ADMIN.getAuthority();
        final String studentAuth = Role.ROLE_STUDENT.getAuthority();
        final String teacherAuth = Role.ROLE_TEACHER.getAuthority();

        // then
        assertThat(adminAuth).isNotEqualTo(studentAuth);
        assertThat(adminAuth).isNotEqualTo(teacherAuth);
        assertThat(studentAuth).isNotEqualTo(teacherAuth);
    }

    @Test
    void values_containsAllRoles() {
        assertThat(Role.values()).containsExactly(Role.ROLE_ADMIN, Role.ROLE_STUDENT, Role.ROLE_TEACHER);
    }
}
