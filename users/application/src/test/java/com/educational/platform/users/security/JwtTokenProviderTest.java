package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
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
    void constructor_secretShorterThan32Bytes_illegalStateException() {
        // given
        final String shortSecret = "0123456789abcdef0123456789abcde";

        // when
        final ThrowableAssert.ThrowingCallable create = () -> new JwtTokenProvider(myUserDetails, VALIDITY, shortSecret);

        // then
        assertThatIllegalStateException()
                .isThrownBy(create)
                .withMessageContaining("at least 32 bytes");
    }

    @Test
    void constructor_secretNotConfigured_randomKeyGeneratedAndTokensWork() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, "");

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void constructor_secretIsBlank_randomKeyGeneratedAndTokensWork() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, "   ");

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void constructor_unconfiguredSecrets_generateIndependentKeys() {
        // given
        final JwtTokenProvider first = new JwtTokenProvider(myUserDetails, VALIDITY, "");
        final JwtTokenProvider second = new JwtTokenProvider(myUserDetails, VALIDITY, "");

        // when
        final String token = first.createToken("username", List.of(Role.ROLE_STUDENT));
        final ThrowableAssert.ThrowingCallable validate = () -> second.validateToken(token);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void createToken_validSecret_tokenContainsSubjectAndAuthorityStrings() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT, Role.ROLE_TEACHER));

        // then
        final Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo("username");
        assertThat(claims.get("auth", List.class)).containsExactly("ROLE_STUDENT", "ROLE_TEACHER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(VALIDITY);
    }

    @Test
    void getUsername_tokenCreatedWithSameSecret_subjectReturned() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final String username = sut.getUsername(token);

        // then
        assertThat(username).isEqualTo("username");
    }

    @Test
    void validateToken_signedWithDifferentKey_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final JwtTokenProvider other = new JwtTokenProvider(myUserDetails, VALIDITY, "another-secret-key-of-32-bytes!!");
        final String token = other.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> sut.validateToken(token);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void validateToken_expiredToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, -1000L, SECRET);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> sut.validateToken(token);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void validateToken_unsignedToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String unsignedToken = Jwts.builder().subject("username").compact();

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> sut.validateToken(unsignedToken);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void validateToken_malformedToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> sut.validateToken("not-a-jwt");

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void getAuthentication_validToken_authoritiesLoadedFromUserDetails() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));
        final UserDetails userDetails = User.withUsername("username")
                .password("password")
                .authorities(Role.ROLE_TEACHER)
                .build();
        when(myUserDetails.loadUserByUsername("username")).thenReturn(userDetails);

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_TEACHER");
    }

    @Test
    void resolveToken_bearerHeader_tokenReturned() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isEqualTo("token-value");
    }

    @Test
    void resolveToken_missingOrNonBearerHeader_null() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, VALIDITY, SECRET);
        final MockHttpServletRequest withoutHeader = new MockHttpServletRequest();
        final MockHttpServletRequest basicHeader = new MockHttpServletRequest();
        basicHeader.addHeader("Authorization", "Basic credentials");

        // when / then
        assertThat(sut.resolveToken(withoutHeader)).isNull();
        assertThat(sut.resolveToken(basicHeader)).isNull();
    }
}
