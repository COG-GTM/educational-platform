package com.educational.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtTokenValidationExceptionTest {

    @Test
    void constructor_keepsMessage() {
        // when
        final JwtTokenValidationException exception = new JwtTokenValidationException("Expired or invalid JWT token");

        // then
        assertThat(exception)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Expired or invalid JWT token");
    }

    @Test
    void constructor_nullMessage_keepsNull() {
        // when
        final JwtTokenValidationException exception = new JwtTokenValidationException(null);

        // then
        assertThat(exception.getMessage()).isNull();
    }
}
