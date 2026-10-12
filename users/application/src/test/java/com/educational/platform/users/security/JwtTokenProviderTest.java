package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final long VALIDITY = 3600000L;

    @Mock
    private MyUserDetails myUserDetails;

    @Test
    void createToken_validInput_signedHs256TokenWithSubjectAndRoles() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_TEACHER));

        // then
        final Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo("username");
        assertThat(claims.get("auth", List.class)).containsExactly("ROLE_TEACHER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(decodeHeader(token)).contains("\"alg\":\"HS256\"");
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void getAuthentication_validToken_authenticationForUser() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));
        when(myUserDetails.loadUserByUsername("username"))
                .thenReturn(User.withUsername("username").password("").authorities(Role.ROLE_STUDENT).build());

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getName()).isEqualTo("username");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
    }

    @Test
    void constructor_secretShorterThan32Bytes_illegalStateException() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, VALIDITY, "secret-key"))
                .withMessageContaining("at least 32 bytes");
    }

    @Test
    void constructor_secretNotConfigured_randomKeyUsed() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, "");
        final JwtTokenProvider other = new JwtTokenProvider(myUserDetails, VALIDITY, " ");

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(sut.validateToken(token)).isTrue();
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> other.validateToken(token));
    }

    @Test
    void validateToken_signedWithOtherKey_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String token = new JwtTokenProvider(myUserDetails, VALIDITY, "fedcba9876543210fedcba9876543210")
                .createToken("username", List.of(Role.ROLE_ADMIN));

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void validateToken_tamperedPayload_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String[] parts = sut.createToken("username", List.of(Role.ROLE_STUDENT)).split("\\.");
        final String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin\",\"auth\":[\"ROLE_ADMIN\"]}".getBytes(StandardCharsets.UTF_8));
        final String tampered = parts[0] + "." + payload + "." + parts[2];

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(tampered));
    }

    @Test
    void validateToken_unsignedToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String unsigned = Jwts.builder().subject("admin").compact();

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(unsigned));
    }

    @Test
    void validateToken_expiredToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String expired = Jwts.builder()
                .subject("username")
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(expired));
    }

    @Test
    void validateToken_malformedToken_jwtTokenValidationException() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken("not-a-jwt"));
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(""));
    }

    @Test
    void resolveToken_bearerHeader_token() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");

        assertThat(sut.resolveToken(request)).isEqualTo("abc");
        assertThat(sut.resolveToken(new MockHttpServletRequest())).isNull();
    }

    private static String decodeHeader(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    }
}
