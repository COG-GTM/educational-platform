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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private MyUserDetails myUserDetails;

    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenProvider(myUserDetails, 3600000L, "secret-key");
    }

    @Test
    void createToken_thenGetUsername_roundTripsSubject() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final String username = sut.getUsername(token);

        // then
        assertThat(token).isNotBlank();
        assertThat(username).isEqualTo("username");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_TEACHER));

        // when
        final boolean valid = sut.validateToken(token);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_malformedToken_jwtTokenValidationException() {
        // given
        final String token = "not-a-valid-jwt";

        // when
        final ThrowableAssert.ThrowingCallable validate = () -> sut.validateToken(token);

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void resolveToken_bearerHeader_returnsToken() {
        // given
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer abc.def.ghi");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isEqualTo("abc.def.ghi");
    }

    @Test
    void resolveToken_missingHeader_returnsNull() {
        // given
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void resolveToken_nonBearerHeader_returnsNull() {
        // given
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void getAuthentication_validToken_authenticationForUser() {
        // given
        final UserDetails userDetails = User.withUsername("username")
                .password("password")
                .authorities(Collections.singletonList(Role.ROLE_STUDENT))
                .build();
        when(myUserDetails.loadUserByUsername("username")).thenReturn(userDetails);
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
    }
}
