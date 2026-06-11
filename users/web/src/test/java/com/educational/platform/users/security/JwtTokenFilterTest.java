package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.security.JwtTokenFilter;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private JwtTokenFilter sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenFilter(jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validToken_authenticationSet() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void doFilterInternal_noToken_noAuthenticationSet() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_invalidToken_errorResponse() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void doFilterInternal_validToken_filterChainContinues() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void doFilterInternal_noToken_filterChainContinues() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void doFilterInternal_invalidToken_filterChainNotCalled() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(filterChain.getRequest()).isNull();
    }

    @Test
    void doFilterInternal_validToken_responseStatusOk() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilterInternal_invalidToken_responseContainsErrorMessage() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(response.getErrorMessage()).contains("Expired or invalid JWT token");
    }

    @Test
    void doFilterInternal_noToken_getAuthenticationNeverCalled() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider, never()).getAuthentication(any());
        verify(jwtTokenProvider, never()).validateToken(any());
    }

    @Test
    void doFilterInternal_invalidToken_getAuthenticationNeverCalled() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider, never()).getAuthentication(any());
    }

    @Test
    void doFilterInternal_existingAuthInContext_overwrittenByNewToken() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication oldAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(oldAuth);

        final Authentication newAuth = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn("new-token");
        when(jwtTokenProvider.validateToken("new-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("new-token")).thenReturn(newAuth);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(newAuth);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotEqualTo(oldAuth);
    }

    @Test
    void doFilterInternal_invalidToken_securityContextCleared() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtTokenProvider.resolveToken(request)).thenReturn("bad-token");
        when(jwtTokenProvider.validateToken("bad-token")).thenThrow(new JwtTokenValidationException("Expired or invalid JWT token"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_noToken_responseStatusOk() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilterInternal_validateTokenThrowsRuntimeException_exceptionPropagates() {
        // given — non-JwtTokenValidationException is NOT caught by the filter
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("some-token");
        when(jwtTokenProvider.validateToken("some-token")).thenThrow(new RuntimeException("unexpected error"));

        // when / then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> sut.doFilterInternal(request, response, filterChain)
        ).isInstanceOf(RuntimeException.class).hasMessageContaining("unexpected error");
    }

    @Test
    void doFilterInternal_validToken_resolveTokenCalledExactlyOnce() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider, times(1)).resolveToken(request);
    }

    @Test
    void doFilterInternal_noToken_validateTokenNeverCalled() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtTokenProvider, never()).validateToken(any());
    }

    @Test
    void doFilterInternal_validateTokenReturnsFalse_noAuthenticationSetAndChainContinues() throws ServletException, IOException {
        // given — validateToken returns false (rather than throwing) → no auth should be set
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("some-token");
        when(jwtTokenProvider.validateToken("some-token")).thenReturn(false);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).getAuthentication(any());
        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilterInternal_getAuthenticationThrows_exceptionPropagates() {
        // given — validateToken succeeds but getAuthentication throws unexpected exception
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenThrow(new RuntimeException("user details error"));

        // when / then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> sut.doFilterInternal(request, response, filterChain)
        ).isInstanceOf(RuntimeException.class).hasMessageContaining("user details error");
    }

    @Test
    void doFilterInternal_validToken_validateCalledBeforeGetAuthentication() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.resolveToken(request)).thenReturn("valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthentication("valid-token")).thenReturn(authentication);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        final var inOrder = inOrder(jwtTokenProvider);
        inOrder.verify(jwtTokenProvider).resolveToken(request);
        inOrder.verify(jwtTokenProvider).validateToken("valid-token");
        inOrder.verify(jwtTokenProvider).getAuthentication("valid-token");
    }

    @Test
    void doFilterInternal_invalidToken_securityContextClearedBeforeErrorSent() throws ServletException, IOException {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtTokenProvider.resolveToken(request)).thenReturn("bad-token");
        when(jwtTokenProvider.validateToken("bad-token")).thenThrow(new JwtTokenValidationException("Invalid"));

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void doFilterInternal_noToken_existingAuthPreserved() throws ServletException, IOException {
        // given — when no token is present, any existing auth in context should remain
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        final Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

        // when
        sut.doFilterInternal(request, response, filterChain);

        // then — the filter does not clear context when there's no token
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
    }
}
