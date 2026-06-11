package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    private JwtTokenFilter sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenFilter(jwtTokenProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validToken_setsAuthenticationAndContinuesChain() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noToken_doesNotSetAuthenticationAndContinuesChain() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_clearsContextAndSendsError() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token"))
                .thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(response).sendError(HttpStatus.BAD_REQUEST.value(), "Expired or invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
    }
}
