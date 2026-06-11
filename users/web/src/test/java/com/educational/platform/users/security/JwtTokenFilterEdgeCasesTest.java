package com.educational.platform.users.security;

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
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterEdgeCasesTest {

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
    void doFilterInternal_tokenPresentButValidationReturnsFalse_doesNotSetAuthentication() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn("some-token");
        when(jwtTokenProvider.validateToken("some-token")).thenReturn(false);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).getAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_emptyStringToken_treatedAsNonNull() throws ServletException, IOException {
        // given
        when(jwtTokenProvider.resolveToken(request)).thenReturn("");
        when(jwtTokenProvider.validateToken("")).thenReturn(false);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
