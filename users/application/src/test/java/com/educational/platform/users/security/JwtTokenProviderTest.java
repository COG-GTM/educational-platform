package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String SECRET_KEY = "test-secret-key";
    private static final long ONE_HOUR_MILLIS = 3_600_000L;

    @Mock
    private MyUserDetails myUserDetails;

    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenProvider(myUserDetails, ONE_HOUR_MILLIS, SECRET_KEY);
    }

    @Test
    void createToken_validUsernameAndRoles_subjectRoundTripsThroughGetUsername() {
        // given - the token's subject is the only identity claim later read back (getAuthentication looks the
        // user up by it), so creating a token and parsing it must preserve the username verbatim
        final String token = sut.createToken("teacher", List.of(Role.ROLE_TEACHER));

        // when
        final String username = sut.getUsername(token);

        // then
        assertThat(token).isNotBlank();
        assertThat(username).isEqualTo("teacher");
    }

    @Test
    void validateToken_tokenIssuedByProvider_returnsTrue() {
        // given
        final String token = sut.createToken("teacher", List.of(Role.ROLE_TEACHER));

        // when / then
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_malformedToken_validationExceptionThrown() {
        // given - an arbitrary non-JWT string cannot be parsed and must be rejected as invalid rather than
        // bubbling up the raw parser exception
        final String malformed = "not-a-jwt-token";

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(malformed))
                .withMessageContaining("Expired or invalid JWT token");
    }

    @Test
    void validateToken_expiredToken_validationExceptionThrown() {
        // given - a provider with negative validity issues an already-expired token; the expiry boundary must
        // be enforced on validation
        final JwtTokenProvider expiringProvider = new JwtTokenProvider(myUserDetails, -10_000L, SECRET_KEY);
        final String expiredToken = expiringProvider.createToken("teacher", List.of(Role.ROLE_TEACHER));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(expiredToken))
                .withMessageContaining("Expired or invalid JWT token");
    }

    @Test
    void validateToken_tokenSignedWithDifferentSecret_validationExceptionThrown() {
        // given - a token whose signature was produced with another secret key must fail validation, proving
        // the signing key is actually enforced
        final JwtTokenProvider foreignProvider = new JwtTokenProvider(myUserDetails, ONE_HOUR_MILLIS, "another-secret-key");
        final String foreignToken = foreignProvider.createToken("teacher", List.of(Role.ROLE_TEACHER));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(foreignToken))
                .withMessageContaining("Expired or invalid JWT token");
    }

    @Test
    void getAuthentication_loadsUserDetailsForTokenSubjectAndCopiesAuthorities() {
        // given - the authentication is built from the freshly loaded UserDetails (not from the token claims),
        // so the principal and authorities must mirror what MyUserDetails returns for the token's subject
        final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TEACHER"));
        final UserDetails userDetails = mock(UserDetails.class);
        doReturn(authorities).when(userDetails).getAuthorities();
        when(myUserDetails.loadUserByUsername("teacher")).thenReturn(userDetails);

        final String token = sut.createToken("teacher", List.of(Role.ROLE_TEACHER));

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        verify(myUserDetails).loadUserByUsername("teacher");
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        assertThat(authentication.getCredentials()).isEqualTo("");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    void resolveToken_bearerAuthorizationHeader_returnsRawToken() {
        // given - the filter strips the "Bearer " scheme prefix and forwards only the raw token
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isEqualTo("abc.def.ghi");
    }

    @Test
    void resolveToken_missingAuthorizationHeader_returnsNull() {
        // given - no Authorization header means there is nothing to authenticate
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        // when / then
        assertThat(sut.resolveToken(request)).isNull();
    }

    @Test
    void resolveToken_nonBearerAuthorizationHeader_returnsNull() {
        // given - only the Bearer scheme is supported; any other authorization scheme is ignored
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // when / then
        assertThat(sut.resolveToken(request)).isNull();
    }
}
