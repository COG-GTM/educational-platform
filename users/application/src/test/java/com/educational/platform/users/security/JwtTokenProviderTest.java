package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private MyUserDetails myUserDetails;

    @Mock
    private HttpServletRequest request;

    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
    }

    @Test
    void createToken_usernameAndRoles_usernameResolvedFromToken() {
        // given
        final String username = "username";

        // when
        final String token = sut.createToken(username, List.of(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo(username);
    }

    @Test
    void validateToken_validToken_true() {
        // given
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final boolean result = sut.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_invalidToken_jwtTokenValidationException() {
        // given
        final String token = "invalid token";

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> sut.validateToken(token);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void validateToken_expiredToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider providerWithNegativeValidity = new JwtTokenProvider(myUserDetails, -1000, "secret-key");
        final String token = providerWithNegativeValidity.createToken("username", List.of(Role.ROLE_STUDENT));

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> providerWithNegativeValidity.validateToken(token);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void getAuthentication_validToken_authenticationWithUserDetails() {
        // given
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));
        final UserDetails userDetails = User.withUsername("username")
                .password("password")
                .authorities(Role.ROLE_STUDENT)
                .build();
        when(myUserDetails.loadUserByUsername("username")).thenReturn(userDetails);

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next()).isEqualTo(Role.ROLE_STUDENT);
    }

    @Test
    void resolveToken_bearerHeader_tokenReturned() {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer token-value");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isEqualTo("token-value");
    }

    @Test
    void resolveToken_noHeader_nullReturned() {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_nonBearerHeader_nullReturned() {
        // given
        when(request.getHeader("Authorization")).thenReturn("Basic credentials");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }
}
