package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SignInRequestTest {

    @Test
    void constructor_validArguments_requestCreated() {
        // when
        final SignInRequest sut = new SignInRequest("username", "password");

        // then
        assertThat(sut.username()).isEqualTo("username");
        assertThat(sut.password()).isEqualTo("password");
    }

    @Test
    void equals_sameValues_equal() {
        // given
        final SignInRequest request1 = new SignInRequest("user", "pass");
        final SignInRequest request2 = new SignInRequest("user", "pass");

        // when / then
        assertThat(request1).isEqualTo(request2);
    }

    @Test
    void equals_differentValues_notEqual() {
        // given
        final SignInRequest request1 = new SignInRequest("user1", "pass1");
        final SignInRequest request2 = new SignInRequest("user2", "pass2");

        // when / then
        assertThat(request1).isNotEqualTo(request2);
    }
}
