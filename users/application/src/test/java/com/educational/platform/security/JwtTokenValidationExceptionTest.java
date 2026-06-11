package com.educational.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenValidationExceptionTest {

    @Test
    void constructor_validMessage_exceptionCreated() {
        // given
        final String message = "Token expired";

        // when
        final JwtTokenValidationException sut = new JwtTokenValidationException(message);

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
        assertThat(sut.getMessage()).isEqualTo("Token expired");
    }

    @Test
    void constructor_emptyMessage_exceptionCreated() {
        // when
        final JwtTokenValidationException sut = new JwtTokenValidationException("");

        // then
        assertThat(sut.getMessage()).isEmpty();
    }

    @Test
    void constructor_nullMessage_exceptionCreated() {
        // when
        final JwtTokenValidationException sut = new JwtTokenValidationException(null);

        // then
        assertThat(sut.getMessage()).isNull();
    }
}
