package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserWebDTOsTest {

    @Test
    void signInRequest_exposesFields() {
        // when
        final SignInRequest request = new SignInRequest("username", "password");

        // then
        assertThat(request.username()).isEqualTo("username");
        assertThat(request.password()).isEqualTo("password");
    }

    @Test
    void signInRequest_equalInstances() {
        assertThat(new SignInRequest("username", "password"))
                .isEqualTo(new SignInRequest("username", "password"));
    }

    @Test
    void signInRequest_differentValues_notEqual() {
        assertThat(new SignInRequest("user1", "password"))
                .isNotEqualTo(new SignInRequest("user2", "password"));
    }

    @Test
    void signInResponse_exposesToken() {
        // when
        final SignInResponse response = new SignInResponse("jwt-token");

        // then
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void signInResponse_equalInstances() {
        assertThat(new SignInResponse("token"))
                .isEqualTo(new SignInResponse("token"));
    }

    @Test
    void signUpRequest_exposesFields() {
        // when
        final SignUpRequest request = new SignUpRequest(RoleDTO.ROLE_STUDENT, "username", "email@test.com", "password");

        // then
        assertThat(request.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(request.username()).isEqualTo("username");
        assertThat(request.email()).isEqualTo("email@test.com");
        assertThat(request.password()).isEqualTo("password");
    }

    @Test
    void signUpRequest_equalInstances() {
        assertThat(new SignUpRequest(RoleDTO.ROLE_STUDENT, "username", "email@test.com", "password"))
                .isEqualTo(new SignUpRequest(RoleDTO.ROLE_STUDENT, "username", "email@test.com", "password"));
    }

    @Test
    void signUpRequest_differentRole_notEqual() {
        assertThat(new SignUpRequest(RoleDTO.ROLE_STUDENT, "username", "email@test.com", "password"))
                .isNotEqualTo(new SignUpRequest(RoleDTO.ROLE_TEACHER, "username", "email@test.com", "password"));
    }
}
