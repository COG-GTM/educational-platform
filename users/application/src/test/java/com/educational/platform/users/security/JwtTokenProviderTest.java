package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private MyUserDetails myUserDetails;

    @Mock
    private HttpServletRequest httpServletRequest;

    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
    }

    @Test
    void createToken_validInput_tokenReturned() {
        // given
        final List<Role> roles = Collections.singletonList(Role.ROLE_STUDENT);

        // when
        final String token = sut.createToken("username", roles);

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void getUsername_validToken_usernameReturned() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final String username = sut.getUsername(token);

        // then
        assertThat(username).isEqualTo("username");
    }

    @Test
    void validateToken_validToken_true() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final boolean result = sut.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_invalidToken_jwtTokenValidationException() {
        // when
        final var validate = new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                sut.validateToken("invalid-token");
            }
        };

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void getAuthentication_validToken_authenticationReturned() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));
        final UserDetails userDetails = User.withUsername("username")
                .password("password")
                .authorities(Collections.singletonList(Role.ROLE_STUDENT))
                .build();
        when(myUserDetails.loadUserByUsername("username")).thenReturn(userDetails);

        // when
        final Authentication result = sut.getAuthentication(token);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("username");
    }

    @Test
    void resolveToken_bearerToken_tokenReturned() {
        // given
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer test-token");

        // when
        final String result = sut.resolveToken(httpServletRequest);

        // then
        assertThat(result).isEqualTo("test-token");
    }

    @Test
    void resolveToken_noAuthorizationHeader_nullReturned() {
        // given
        when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

        // when
        final String result = sut.resolveToken(httpServletRequest);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_nonBearerToken_nullReturned() {
        // given
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic test-token");

        // when
        final String result = sut.resolveToken(httpServletRequest);

        // then
        assertThat(result).isNull();
    }
}
