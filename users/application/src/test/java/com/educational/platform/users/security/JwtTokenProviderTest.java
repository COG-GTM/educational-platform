package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class JwtTokenProviderTest {

    private static final String STRONG_KEY = "0123456789abcdef0123456789abcdef";

    private final MyUserDetails myUserDetails = mock(MyUserDetails.class);

    @Test
    void constructor_keyShorterThan256Bits_rejected() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, "secret-key"))
                .withMessageContaining("at least 256 bits");
    }

    @Test
    void constructor_keyOneByteShort_rejected() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY.substring(1)));
    }

    @Test
    void createToken_256BitKey_signedWithHs256AndValid() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);

        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(header(token)).contains("\"alg\":\"HS256\"");
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_tokenSignedWithOtherKey_rejected() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, STRONG_KEY);
        final JwtTokenProvider other = new JwtTokenProvider(myUserDetails, 3600000, "fedcba9876543210fedcba9876543210");

        final String token = other.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void validateToken_tokenForgedWithFormerDefaultKey_rejected() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "");
        final String forged = Jwts.builder()
                .setSubject("admin")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, "secret-key".getBytes(StandardCharsets.UTF_8))
                .compact();

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(forged));
    }

    @Test
    void constructor_noKeyConfigured_usesRandomPerInstanceKey() {
        final JwtTokenProvider first = new JwtTokenProvider(myUserDetails, 3600000, "");
        final JwtTokenProvider second = new JwtTokenProvider(myUserDetails, 3600000, null);

        final String token = first.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(first.validateToken(token)).isTrue();
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> second.validateToken(token));
    }

    private static String header(String token) {
        return new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))), StandardCharsets.UTF_8);
    }
}
