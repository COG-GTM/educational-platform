package com.educational.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenValidationExceptionTest {

    @Test
    void getMessage_returnsProvidedMessage() {
        // given
        final JwtTokenValidationException sut = new JwtTokenValidationException("token expired");

        // when
        final String message = sut.getMessage();

        // then
        assertThat(message).isEqualTo("token expired");
    }

    @Test
    void isRuntimeException() {
        // given
        final JwtTokenValidationException sut = new JwtTokenValidationException("token expired");

        // then
        assertThat(sut).isInstanceOf(RuntimeException.class);
    }
}
