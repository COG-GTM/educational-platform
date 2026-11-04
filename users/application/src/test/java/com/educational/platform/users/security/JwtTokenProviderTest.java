package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
    void createToken_claimsAndLifetime_preserved() {
        // given
        final Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT, Role.ROLE_TEACHER));
        final Instant after = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        final var jwt = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        // then
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(jwt.getPayload().getSubject()).isEqualTo("username");
        assertThat(jwt.getPayload().get("auth")).isEqualTo(List.of(
                Map.of("authority", "ROLE_STUDENT"), Map.of("authority", "ROLE_TEACHER")));
        assertThat(jwt.getPayload().getIssuedAt().toInstant()).isBetween(before, after);
        assertThat(jwt.getPayload().getExpiration().getTime() - jwt.getPayload().getIssuedAt().getTime())
                .isEqualTo(3600000);
    }

    @Test
    void createToken_emptyRoles_validTokenWithEmptyAuthorities() {
        // when
        final String token = sut.createToken("username", List.of());
        final var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // then
        assertThat(claims.get("auth")).isEqualTo(List.of());
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void createToken_longSecret_stillUsesHs256() {
        // given
        final String secret = "0123456789abcdef".repeat(4);
        final JwtTokenProvider provider = new JwtTokenProvider(null, 3600000, secret);

        // when
        final String token = provider.createToken("username", List.of(Role.ROLE_STUDENT));
        final var jwt = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        // then
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_tamperedPayload_rejected() {
        final String[] parts = sut.createToken("username", List.of(Role.ROLE_STUDENT)).split("\\.");
        final String forgedPayload = base64Url("{\"sub\":\"admin\",\"auth\":[{\"authority\":\"ROLE_ADMIN\"}]}");
        final String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(forged));
        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> sut.getUsername(forged));
    }

    @Test
    void validateToken_unsignedToken_rejected() {
        final String unsigned = base64Url("{\"alg\":\"none\"}") + "." + base64Url("{\"sub\":\"admin\"}") + ".";

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(unsigned));
        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> sut.getUsername(unsigned));
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

    @ParameterizedTest
    @ValueSource(strings = {"HS384", "HS512"})
    void validateToken_otherAlgorithmWithSameKey_rejected(String algorithm) {
        // given
        final String secret = "0123456789abcdef".repeat(4);
        final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        final JwtTokenProvider provider = new JwtTokenProvider(null, 3600000, secret);
        final var builder = Jwts.builder().subject("username");
        final String token = (algorithm.equals("HS384")
                ? builder.signWith(key, Jwts.SIG.HS384)
                : builder.signWith(key, Jwts.SIG.HS512)).compact();

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> provider.validateToken(token));
        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> provider.getUsername(token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "not-a-jwt", "a.b", "a.b.c", "a.b.c.d"})
    void validateToken_invalidInput_validationException(String token) {
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token))
                .withMessage("Expired or invalid JWT token");
    }

    @Test
    void validateToken_notYetValid_rejected() {
        // given
        final String token = Jwts.builder()
                .subject("username")
                .notBefore(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token))
                .withMessage("Expired or invalid JWT token");
    }

    @Test
    void validateToken_expiredToken_rejected() {
        final String token = new JwtTokenProvider(null, -1000, SECRET)
                .createToken("username", List.of(Role.ROLE_STUDENT));

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(token));
        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> sut.getUsername(token));
    }

    @Test
    void constructor_weakSecret_rejected() {
        assertThatExceptionOfType(WeakKeyException.class)
                .isThrownBy(() -> new JwtTokenProvider(null, 3600000, "secret-key"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0123456789abcdef0123456789abcde", "ééééééééééééééé"})
    void constructor_secretBelow32Utf8Bytes_rejected(String secret) {
        assertThatExceptionOfType(WeakKeyException.class)
                .isThrownBy(() -> new JwtTokenProvider(null, 3600000, secret));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0123456789abcdef0123456789abcdef", "éééééééééééééééé"})
    void constructor_secretExactly32Utf8Bytes_acceptedAcrossInstances(String secret) {
        // given
        final JwtTokenProvider provider = new JwtTokenProvider(null, 3600000, secret);
        final JwtTokenProvider other = new JwtTokenProvider(null, 3600000, secret);

        // when
        final String token = provider.createToken("usuário", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(other.validateToken(token)).isTrue();
        assertThat(other.getUsername(token)).isEqualTo("usuário");
        assertThat(Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token).getPayload().getSubject()).isEqualTo("usuário");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void constructor_noSecret_randomKeyUsed(String secret) {
        final JwtTokenProvider other = new JwtTokenProvider(null, 3600000, secret);
        final String token = other.createToken("username", List.of(Role.ROLE_STUDENT));

        assertThat(other.validateToken(token)).isTrue();
        assertThat(other.getUsername(token)).isEqualTo("username");
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> new JwtTokenProvider(null, 3600000, secret).validateToken(token));
    }

    @Test
    void getAuthentication_validToken_loadsUserAndUsesCurrentAuthorities() {
        // given
        final MyUserDetails userDetailsService = mock(MyUserDetails.class);
        final var userDetails = User.withUsername("username").password("password").roles("TEACHER").build();
        when(userDetailsService.loadUserByUsername("username")).thenReturn(userDetails);
        final JwtTokenProvider provider = new JwtTokenProvider(userDetailsService, 3600000, SECRET);
        final String token = provider.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final var authentication = provider.getAuthentication(token);

        // then
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        assertThat(authentication.getCredentials()).isEqualTo("");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TEACHER");
        verify(userDetailsService).loadUserByUsername("username");
        verifyNoMoreInteractions(userDetailsService);
    }

    @Test
    void getAuthentication_invalidSignature_doesNotLoadUser() {
        // given
        final MyUserDetails userDetailsService = mock(MyUserDetails.class);
        final JwtTokenProvider provider = new JwtTokenProvider(userDetailsService, 3600000, SECRET);
        final String token = new JwtTokenProvider(null, 3600000, SECRET + "-other")
                .createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> provider.getAuthentication(token));
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void getAuthentication_unknownUser_propagatesUserNotFound() {
        // given
        final MyUserDetails userDetailsService = mock(MyUserDetails.class);
        final var exception = new UsernameNotFoundException("User not found");
        when(userDetailsService.loadUserByUsername("username")).thenThrow(exception);
        final JwtTokenProvider provider = new JwtTokenProvider(userDetailsService, 3600000, SECRET);
        final String token = provider.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> provider.getAuthentication(token))
                .isSameAs(exception);
        verify(userDetailsService).loadUserByUsername("username");
        verifyNoMoreInteractions(userDetailsService);
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
