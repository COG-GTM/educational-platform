package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenProperties;
import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private MyUserDetails myUserDetails;

    private JwtTokenProvider provider(String secretKey, String previousSecretKey) {
        final JwtTokenProperties properties = new JwtTokenProperties();
        properties.setSecretKey(secretKey);
        properties.setPreviousSecretKey(previousSecretKey);
        return new JwtTokenProvider(myUserDetails, properties);
    }

    @Test
    void validateToken_signedWithCurrentKey_valid() {
        // given
        final JwtTokenProvider sut = provider("current-key", null);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final boolean valid = sut.validateToken(token);

        // then
        assertThat(valid).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_signedWithPreviousKeyDuringRotation_valid() {
        // given
        final JwtTokenProvider oldProvider = provider("old-key", null);
        final String token = oldProvider.createToken("username", List.of(Role.ROLE_STUDENT));
        final JwtTokenProvider sut = provider("new-key", "old-key");

        // when
        final boolean valid = sut.validateToken(token);

        // then
        assertThat(valid).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_signedWithUnknownKey_jwtTokenValidationException() {
        // given
        final JwtTokenProvider otherProvider = provider("unknown-key", null);
        final String token = otherProvider.createToken("username", List.of(Role.ROLE_STUDENT));
        final JwtTokenProvider sut = provider("new-key", "old-key");

        // when, then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void validateToken_signedWithOldKeyNoRotationWindow_jwtTokenValidationException() {
        // given
        final JwtTokenProvider oldProvider = provider("old-key", null);
        final String token = oldProvider.createToken("username", List.of(Role.ROLE_STUDENT));
        final JwtTokenProvider sut = provider("new-key", null);

        // when, then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }
}
