package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @InjectMocks
    private JwtTokenFilter sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validToken_authenticationSetAndChainContinues() throws Exception {
        // given - a valid bearer token is resolved, validated, and turned into an Authentication that
        // the filter must publish to the SecurityContext before passing the request down the chain
        final Authentication authentication = authenticationFor("student");
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void doFilterInternal_missingToken_chainContinuesWithoutAuthentication() throws Exception {
        // given - no Authorization header, so resolveToken returns null; the filter must not attempt
        // validation and must leave the context unauthenticated while still continuing the chain
        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void doFilterInternal_invalidToken_contextClearedErrorSentAndChainHalted() throws Exception {
        // given - validation rejects the token; the filter must clear any pre-existing authentication,
        // respond 400 with the validation message, and NOT pass the request down the chain
        SecurityContextHolder.getContext().setAuthentication(authenticationFor("stale"));
        when(jwtTokenProvider.resolveToken(request)).thenReturn("bad-token");
        when(jwtTokenProvider.validateToken("bad-token"))
                .thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(response).sendError(400, "Expired or invalid JWT token");
        verify(filterChain, never()).doFilter(request, response);
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
    }

    @Test
    void doFilterInternal_tokenPresentButRejectedByValidation_chainContinuesWithoutAuthentication() throws Exception {
        // given - a token is resolved but validateToken reports it invalid by returning false (the other
        // negative outcome of the boolean contract, distinct from throwing); the filter must treat a falsy
        // result as "not authenticated", never load an Authentication, leave the context untouched, and
        // still let the request proceed down the chain without sending an error
        when(jwtTokenProvider.resolveToken(request)).thenReturn("present-token");
        when(jwtTokenProvider.validateToken("present-token")).thenReturn(false);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).getAuthentication(anyString());
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    private Authentication authenticationFor(String username) {
        final UserDetails principal = User.withUsername(username)
                .password("password")
                .authorities("ROLE_STUDENT")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities());
    }
}
