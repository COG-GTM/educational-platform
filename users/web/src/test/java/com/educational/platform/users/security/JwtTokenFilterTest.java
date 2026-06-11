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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

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
    void doFilterInternal_validToken_authenticationSet() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(httpServletRequest)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        final var authentication = new UsernamePasswordAuthenticationToken("username", "", Collections.singletonList(Role.ROLE_STUDENT));
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("username");
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    void doFilterInternal_noToken_noAuthentication() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(httpServletRequest)).thenReturn(null);

        // when
        sut.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(httpServletRequest, httpServletResponse);
    }

    @Test
    void doFilterInternal_invalidToken_contextCleared() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(httpServletRequest)).thenReturn("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(httpServletRequest, httpServletResponse, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(httpServletResponse).sendError(HttpStatus.BAD_REQUEST.value(), "Expired or invalid JWT token");
        verify(filterChain, never()).doFilter(httpServletRequest, httpServletResponse);
    }
}
