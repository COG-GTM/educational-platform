package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link SignInResponse} record.
 */
public class SignInResponseTest {

    @Test
    void token_returnsConstructedValue() {
        final SignInResponse response = new SignInResponse("jwt-token-value");
        assertThat(response.token()).isEqualTo("jwt-token-value");
    }

    @Test
    void equalInstances() {
        assertThat(new SignInResponse("token")).isEqualTo(new SignInResponse("token"));
    }

    @Test
    void differentTokens_notEqual() {
        assertThat(new SignInResponse("a")).isNotEqualTo(new SignInResponse("b"));
    }

    @Test
    void nullToken_handledGracefully() {
        final SignInResponse response = new SignInResponse(null);
        assertThat(response.token()).isNull();
    }
}
