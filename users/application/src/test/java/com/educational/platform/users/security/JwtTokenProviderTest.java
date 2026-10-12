package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void getAuthentication_validToken_authoritiesTakenFromUserDetailsNotFromTokenClaim() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));
        final UserDetails userDetails = User.withUsername("username").password("").authorities(Role.ROLE_ADMIN).build();
        when(myUserDetails.loadUserByUsername("username")).thenReturn(userDetails);

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        assertThat(authentication.getCredentials()).isEqualTo("");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void getAuthentication_tokenSignedWithOtherKey_jwtExceptionAndUserDetailsNotLoaded() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String token = new JwtTokenProvider(myUserDetails, VALIDITY, "fedcba9876543210fedcba9876543210")
                .createToken("username", List.of(Role.ROLE_ADMIN));

        // then
        assertThatExceptionOfType(JwtException.class).isThrownBy(() -> sut.getAuthentication(token));
        verifyNoInteractions(myUserDetails);
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
    void constructor_nullSecret_randomKeyUsed() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, null);
        final JwtTokenProvider other = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

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

    @Test
    void createToken_multipleRoles_authClaimListsAllAuthoritiesInOrder() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT, Role.ROLE_TEACHER, Role.ROLE_ADMIN));

        // then
        assertThat(parseClaims(token).get("auth", List.class)).containsExactly("ROLE_STUDENT", "ROLE_TEACHER", "ROLE_ADMIN");
    }

    @Test
    void createToken_noRoles_emptyAuthClaim() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        // when
        final String token = sut.createToken("username", List.of());

        // then
        assertThat(parseClaims(token).get("auth", List.class)).isEmpty();
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void createToken_configuredValidity_expirationIsIssuedAtPlusValidity() {
        // given
        final long validity = 120_000L;
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, validity, SECRET);

        // when
        final Claims claims = parseClaims(sut.createToken("username", List.of(Role.ROLE_STUDENT)));

        // then
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(validity);
    }

    @Test
    void createToken_negativeValidity_tokenAlreadyExpired() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, -60_000L, SECRET);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token))
                .withMessage("Expired or invalid JWT token");
    }

    @Test
    void validateToken_tokenIssuedByOtherInstanceWithSameSecret_true() {
        // given
        final String token = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET)
                .createToken("username", List.of(Role.ROLE_TEACHER));
        final JwtTokenProvider restarted = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        // then
        assertThat(restarted.validateToken(token)).isTrue();
        assertThat(restarted.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_tamperedSignature_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String[] parts = sut.createToken("username", List.of(Role.ROLE_STUDENT)).split("\\.");
        final char last = parts[2].charAt(parts[2].length() - 1);
        final String flipped = parts[2].substring(0, parts[2].length() - 1) + (last == 'A' ? 'B' : 'A');
        final String tampered = parts[0] + "." + parts[1] + "." + flipped;

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(tampered));
    }

    @Test
    void validateToken_signatureStripped_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String[] parts = sut.createToken("username", List.of(Role.ROLE_STUDENT)).split("\\.");
        final String stripped = parts[0] + "." + parts[1] + ".";

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(stripped));
    }

    @Test
    void validateToken_nullToken_jwtTokenValidationException() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(() -> sut.validateToken(null));
    }

    @Test
    void constructor_secretExactly32Bytes_accepted() {
        assertThat(SECRET.getBytes(StandardCharsets.UTF_8)).hasSize(32);

        assertThatNoException().isThrownBy(() -> new JwtTokenProvider(myUserDetails, VALIDITY, SECRET));
    }

    @Test
    void constructor_secret31Bytes_illegalStateException() {
        final String secret = SECRET.substring(1);
        assertThat(secret.getBytes(StandardCharsets.UTF_8)).hasSize(31);

        assertThatIllegalStateException()
                .isThrownBy(() -> new JwtTokenProvider(myUserDetails, VALIDITY, secret))
                .withMessage("security.jwt.token.secret-key must be at least 32 bytes for HS256");
    }

    @Test
    void constructor_multiByteSecret_lengthMeasuredInUtf8BytesNotChars() {
        // given: 16 chars x 2 bytes each = 32 bytes
        final String secret = "\u00e4".repeat(16);
        assertThat(secret).hasSize(16);
        assertThat(secret.getBytes(StandardCharsets.UTF_8)).hasSize(32);

        // when
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, secret);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(new JwtTokenProvider(myUserDetails, VALIDITY, secret).getUsername(token)).isEqualTo("username");
    }

    @Test
    void constructor_secretNotConfigured_warnsAboutRandomKey() {
        // given
        final ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            // when
            new JwtTokenProvider(myUserDetails, VALIDITY, "");

            // then
            assertThat(appender.list)
                    .filteredOn(event -> event.getLevel() == Level.WARN)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .singleElement().asString()
                    .contains("security.jwt.token.secret-key is not set")
                    .contains("random HS256 key");
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void constructor_secretConfigured_noWarning() {
        // given
        final ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            // when
            new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

            // then
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN)).isEmpty();
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void resolveToken_nonBearerAuthorization_null() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        assertThat(sut.resolveToken(requestWithAuthorization("Basic dXNlcjpwYXNz"))).isNull();
        assertThat(sut.resolveToken(requestWithAuthorization("bearer abc"))).isNull();
        assertThat(sut.resolveToken(requestWithAuthorization("Bearer"))).isNull();
        assertThat(sut.resolveToken(requestWithAuthorization("abc"))).isNull();
    }

    @Test
    void resolveToken_bearerPrefixOnly_emptyToken() {
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        assertThat(sut.resolveToken(requestWithAuthorization("Bearer "))).isEmpty();
    }

    private static MockHttpServletRequest requestWithAuthorization(String value) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", value);
        return request;
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(JwtTokenProvider.class)).addAppender(appender);
        return appender;
    }

    private static void detachAppender(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(JwtTokenProvider.class)).detachAppender(appender);
        appender.stop();
    }

    private static String decodeHeader(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
    }
}
