package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterPreExistingAuthTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private FilterChain filterChain;

    private JwtTokenFilter sut;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        sut = new JwtTokenFilter(jwtTokenProvider);
    }

    @Test
    void doFilterInternal_invalidTokenWithPreExistingAuth_contextCleared() throws ServletException, IOException {
        // given
        final var preExistingAuth = new UsernamePasswordAuthenticationToken(
                "existing-user", "", Collections.singletonList(Role.ROLE_STUDENT));
        SecurityContextHolder.getContext().setAuthentication(preExistingAuth);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        when(jwtTokenProvider.resolveToken(httpServletRequest)).thenReturn("bad-token");
        when(jwtTokenProvider.validateToken("bad-token"))
                .thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    void doFilterInternal_noTokenWithPreExistingAuth_authPreserved() throws ServletException, IOException {
        // given
        final var preExistingAuth = new UsernamePasswordAuthenticationToken(
                "existing-user", "", Collections.singletonList(Role.ROLE_STUDENT));
        SecurityContextHolder.getContext().setAuthentication(preExistingAuth);

        when(jwtTokenProvider.resolveToken(httpServletRequest)).thenReturn(null);

        // when
        sut.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing-user");
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    void doFilterInternal_validTokenReplacesPreExistingAuth_newAuthSet() throws ServletException, IOException {
        // given
        final var preExistingAuth = new UsernamePasswordAuthenticationToken(
                "old-user", "", Collections.singletonList(Role.ROLE_STUDENT));
        SecurityContextHolder.getContext().setAuthentication(preExistingAuth);

        when(jwtTokenProvider.resolveToken(httpServletRequest)).thenReturn("new-valid-token");
        when(jwtTokenProvider.validateToken("new-valid-token")).thenReturn(true);
        final var newAuth = new UsernamePasswordAuthenticationToken(
                "new-user", "", Collections.singletonList(Role.ROLE_TEACHER));
        when(jwtTokenProvider.getAuthentication("new-valid-token")).thenReturn(newAuth);

        // when
        sut.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("new-user");
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }
}
