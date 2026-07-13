package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

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
    void doFilterInternal_validToken_setsAuthenticationAndContinuesChain() throws Exception {
        // given
        final Authentication authentication = new UsernamePasswordAuthenticationToken("username", "", Collections.emptyList());
        when(jwtTokenProvider.resolveToken(request)).thenReturn("token");
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noToken_continuesChainWithoutAuthentication() throws Exception {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_sendsBadRequestAndStopsChain() throws Exception {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn("token");
        when(jwtTokenProvider.validateToken("token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(response).sendError(HttpStatus.BAD_REQUEST.value(), "Expired or invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
    }
}
