package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SignUpRequestTest {

    @Test
    void constructor_studentRole_requestCreated() {
        // when
        final SignUpRequest sut = new SignUpRequest(RoleDTO.ROLE_STUDENT, "john", "john@example.com", "P@ssw0rd!");

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(sut.username()).isEqualTo("john");
        assertThat(sut.email()).isEqualTo("john@example.com");
        assertThat(sut.password()).isEqualTo("P@ssw0rd!");
    }

    @Test
    void constructor_teacherRole_requestCreated() {
        // when
        final SignUpRequest sut = new SignUpRequest(RoleDTO.ROLE_TEACHER, "teacher", "teacher@example.com", "SecurePass1!");

        // then
        assertThat(sut.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void equals_sameValues_equal() {
        // given
        final SignUpRequest request1 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "john", "john@example.com", "pass");
        final SignUpRequest request2 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "john", "john@example.com", "pass");

        // when / then
        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void equals_differentValues_notEqual() {
        // given
        final SignUpRequest request1 = new SignUpRequest(RoleDTO.ROLE_STUDENT, "john", "john@example.com", "pass");
        final SignUpRequest request2 = new SignUpRequest(RoleDTO.ROLE_TEACHER, "jane", "jane@example.com", "pass");

        // when / then
        assertThat(request1).isNotEqualTo(request2);
    }
}
