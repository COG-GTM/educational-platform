package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SignInResponseTest {

    @Test
    void constructor_validToken_responseCreated() {
        // when
        final SignInResponse sut = new SignInResponse("jwt-token-value");

        // then
        assertThat(sut.token()).isEqualTo("jwt-token-value");
    }

    @Test
    void constructor_nullToken_responseCreated() {
        // when
        final SignInResponse sut = new SignInResponse(null);

        // then
        assertThat(sut.token()).isNull();
    }

    @Test
    void equals_sameToken_equal() {
        // given
        final SignInResponse response1 = new SignInResponse("token");
        final SignInResponse response2 = new SignInResponse("token");

        // when / then
        assertThat(response1).isEqualTo(response2);
    }
}
