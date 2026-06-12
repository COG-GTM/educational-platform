package com.educational.platform.users.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderResolveTokenTest {

    @Mock
    private MyUserDetails myUserDetails;

    @Mock
    private HttpServletRequest request;

    @Test
    void resolveToken_bearerTokenPresent_returnsTokenWithoutPrefix() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
        when(request.getHeader("Authorization")).thenReturn("Bearer my-jwt-token");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isEqualTo("my-jwt-token");
    }

    @Test
    void resolveToken_noAuthorizationHeader_returnsNull() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_nonBearerPrefix_returnsNull() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_emptyBearerValue_returnsEmptyString() {
        // given
        final JwtTokenProvider sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isEmpty();
    }
}
