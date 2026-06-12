package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderEdgeCaseTest {

    @Mock
    private MyUserDetails myUserDetails;

    @Test
    void createToken_multipleRoles_tokenCreatedSuccessfully() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
        final List<Role> roles = List.of(Role.ROLE_STUDENT, Role.ROLE_TEACHER);

        // when
        final String token = sut.createToken("multiuser", roles);

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("multiuser");
    }

    @Test
    void validateToken_expiredToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 0, "secret-key");
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final var validate = new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                sut.validateToken(token);
            }
        };

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void validateToken_tokenWithWrongSecret_jwtTokenValidationException() {
        // given
        final JwtTokenProvider creator = new JwtTokenProvider(myUserDetails, 3600000, "creator-secret");
        final JwtTokenProvider validator = new JwtTokenProvider(myUserDetails, 3600000, "validator-secret");
        final String token = creator.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final var validate = new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                validator.validateToken(token);
            }
        };

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void validateToken_emptyToken_jwtTokenValidationException() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");

        // when
        final var validate = new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                sut.validateToken("");
            }
        };

        // then
        assertThatExceptionOfType(JwtTokenValidationException.class).isThrownBy(validate);
    }

    @Test
    void getAuthentication_validToken_authoritiesPreserved() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
        final String token = sut.createToken("testuser", Collections.singletonList(Role.ROLE_TEACHER));
        final UserDetails userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.singletonList(Role.ROLE_TEACHER))
                .build();
        when(myUserDetails.loadUserByUsername("testuser")).thenReturn(userDetails);

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_TEACHER");
    }

    @Test
    void createToken_emptyRoles_tokenCreatedSuccessfully() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");

        // when
        final String token = sut.createToken("username", Collections.emptyList());

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }
}
