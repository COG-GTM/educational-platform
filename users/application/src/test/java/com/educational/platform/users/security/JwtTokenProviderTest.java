package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JwtTokenProviderTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef-test-secret";

    private final JwtTokenProvider sut = new JwtTokenProvider(null, 3600000, SECRET);

    @Test
    void createToken_validToken_validatedAndUsernameResolved() {
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_tamperedPayload_rejected() {
        final String[] parts = sut.createToken("username", List.of(Role.ROLE_STUDENT)).split("\\.");
        final String forgedPayload = base64Url("{\"sub\":\"admin\",\"auth\":[{\"authority\":\"ROLE_ADMIN\"}]}");
        final String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(forged));
    }

    @Test
    void validateToken_unsignedToken_rejected() {
        final String unsigned = base64Url("{\"alg\":\"none\"}") + "." + base64Url("{\"sub\":\"admin\"}") + ".";

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(unsigned));
    }

    @Test
    void validateToken_signedWithDifferentKey_rejected() {
        final String token = new JwtTokenProvider(null, 3600000, SECRET + "-other")
                .createToken("username", List.of(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void validateToken_otherAlgorithm_rejected() {
        final String token = Jwts.builder()
                .subject("username")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Jwts.SIG.HS512.key().build())
                .compact();

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void validateToken_expiredToken_rejected() {
        final String token = new JwtTokenProvider(null, -1000, SECRET)
                .createToken("username", List.of(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void constructor_weakSecret_rejected() {
        assertThatExceptionOfType(WeakKeyException.class)
                .isThrownBy(() -> new JwtTokenProvider(null, 3600000, "secret-key"));
    }

    @Test
    void constructor_noSecret_randomKeyUsed() {
        final JwtTokenProvider other = new JwtTokenProvider(null, 3600000, "");
        final String token = other.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(other.validateToken(token)).isTrue();
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> new JwtTokenProvider(null, 3600000, "").validateToken(token));
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
