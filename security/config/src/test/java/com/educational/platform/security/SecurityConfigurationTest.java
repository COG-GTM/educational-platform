package com.educational.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    private final SecurityConfiguration sut = new SecurityConfiguration();

    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        // when
        final PasswordEncoder encoder = sut.passwordEncoder();

        // then
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodesAndMatchesRawPassword() {
        // given
        final PasswordEncoder encoder = sut.passwordEncoder();
        final String rawPassword = "s3cr3t-password";

        // when
        final String encoded = encoder.encode(rawPassword);

        // then
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        assertThat(encoder.matches("wrong-password", encoded)).isFalse();
    }
}
