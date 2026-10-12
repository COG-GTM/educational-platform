package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    private static final String SECRET = randomSecret();

    @Mock
    private MyUserDetails myUserDetails;

    @Test
    void constructor_missingSecretKey_failsFast() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, ""))
                .withMessageContaining(JwtTokenProvider.SECRET_KEY_PROPERTY);
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, null));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "   "));
    }

    @Test
    void constructor_shortSecretKey_failsFast() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "secret-key"))
                .withMessageContaining("at least 32 bytes");
    }

    private static String randomSecret() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    @Test
    void createToken_configuredSecretKey_tokenValidatesWithSameKey() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, SECRET);

        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_tokenSignedWithOtherKey_rejected() {
        final JwtTokenProvider issuer = new JwtTokenProvider(myUserDetails, 3600000, randomSecret());
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, SECRET);

        final String token = issuer.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token));
    }

}
