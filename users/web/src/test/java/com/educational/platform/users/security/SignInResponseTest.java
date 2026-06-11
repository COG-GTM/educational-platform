package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SignInResponseTest {

    @Test
    void constructor_withToken_accessorReturnsToken() {
        // when
        final SignInResponse response = new SignInResponse("jwt-token-123");

        // then
        assertThat(response.token()).isEqualTo("jwt-token-123");
    }

    @Test
    void constructor_nullToken_accessorReturnsNull() {
        // when
        final SignInResponse response = new SignInResponse(null);

        // then
        assertThat(response.token()).isNull();
    }

    @Test
    void constructor_emptyToken_accessorReturnsEmpty() {
        // when
        final SignInResponse response = new SignInResponse("");

        // then
        assertThat(response.token()).isEmpty();
    }

    @Test
    void equality_sameToken_equal() {
        // given
        final SignInResponse response1 = new SignInResponse("token");
        final SignInResponse response2 = new SignInResponse("token");

        // then
        assertThat(response1).isEqualTo(response2);
    }

    @Test
    void equality_differentTokens_notEqual() {
        // given
        final SignInResponse response1 = new SignInResponse("token1");
        final SignInResponse response2 = new SignInResponse("token2");

        // then
        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    void toString_containsToken() {
        // given
        final SignInResponse response = new SignInResponse("my-jwt-token");

        // when
        final String str = response.toString();

        // then
        assertThat(str).contains("my-jwt-token");
    }

    @Test
    void hashCode_sameValues_equal() {
        // given
        final SignInResponse response1 = new SignInResponse("token");
        final SignInResponse response2 = new SignInResponse("token");

        // then
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
}
