package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
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
    void createToken_thenGetUsername_returnsSubject() {
        // when
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        // given
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));

        // then
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_invalidToken_throwsValidationException() {
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken("invalid-token"));
    }

    @Test
    void resolveToken_bearerHeader_returnsToken() {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer abc123");

        // then
        assertThat(sut.resolveToken(request)).isEqualTo("abc123");
    }

    @Test
    void resolveToken_noBearerPrefix_returnsNull() {
        // given
        when(request.getHeader("Authorization")).thenReturn("abc123");

        // then
        assertThat(sut.resolveToken(request)).isNull();
    }

    @Test
    void getAuthentication_loadsUserDetailsForTokenSubject() {
        // given
        final String token = sut.createToken("username", List.of(Role.ROLE_STUDENT));
        final UserDetails userDetails = User.withUsername("username")
                .password("password")
                .authorities(Collections.singletonList(Role.ROLE_STUDENT))
                .build();
        when(myUserDetails.loadUserByUsername("username")).thenReturn(userDetails);

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
    }
}
