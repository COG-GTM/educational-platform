package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that {@link JwtTokenProvider#validateToken(String)} throws
 * {@link JwtTokenValidationException} for an expired token.
 */
@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderExpiredTokenTest {

    @Mock
    private MyUserDetails myUserDetails;

    @Test
    void validateToken_expiredToken_throwsJwtTokenValidationException() throws InterruptedException {
        // given - provider with 1ms validity
        final JwtTokenProvider provider = new JwtTokenProvider(myUserDetails, 1, "secret-key");
        final String token = provider.createToken("username", List.of(Role.ROLE_STUDENT));

        // allow token to expire
        Thread.sleep(50);

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> provider.validateToken(token))
                .withMessageContaining("Expired or invalid JWT token");
    }

    @Test
    void validateToken_emptyString_throwsJwtTokenValidationException() {
        // given
        final JwtTokenProvider provider = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> provider.validateToken(""));
    }

    @Test
    void validateToken_tokenSignedWithDifferentKey_throwsJwtTokenValidationException() {
        // given
        final JwtTokenProvider provider1 = new JwtTokenProvider(myUserDetails, 3600000, "secret-key-1");
        final JwtTokenProvider provider2 = new JwtTokenProvider(myUserDetails, 3600000, "secret-key-2");
        final String token = provider1.createToken("username", List.of(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> provider2.validateToken(token));
    }
}
