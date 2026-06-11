package com.educational.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SecurityConfiguration#passwordEncoder()} returns a
 * BCrypt encoder and that it can round-trip encode/verify a password.
 */
public class SecurityConfigurationPasswordEncoderTest {

    private final SecurityConfiguration sut = new SecurityConfiguration();

    @Test
    void passwordEncoder_returnsBCryptEncoder() {
        // when
        final PasswordEncoder encoder = sut.passwordEncoder();

        // then
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encodedPasswordMatchesRawPassword() {
        // given
        final PasswordEncoder encoder = sut.passwordEncoder();
        final String raw = "testPassword123!";

        // when
        final String encoded = encoder.encode(raw);

        // then
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void passwordEncoder_differentPasswords_doNotMatch() {
        // given
        final PasswordEncoder encoder = sut.passwordEncoder();

        // when
        final String encoded = encoder.encode("password1");

        // then
        assertThat(encoder.matches("password2", encoded)).isFalse();
    }

    @Test
    void passwordEncoder_encodeSamePasswordTwice_producesDifferentHashes() {
        // given
        final PasswordEncoder encoder = sut.passwordEncoder();
        final String raw = "testPassword";

        // when
        final String encoded1 = encoder.encode(raw);
        final String encoded2 = encoder.encode(raw);

        // then — BCrypt uses random salt so encoded values differ
        assertThat(encoded1).isNotEqualTo(encoded2);
    }
}
