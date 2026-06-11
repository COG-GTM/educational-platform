package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderEdgeCasesTest {

    @Mock
    private MyUserDetails myUserDetails;

    @Mock
    private HttpServletRequest request;

    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        sut = new JwtTokenProvider(myUserDetails, 3600000, "test-secret-key");
    }

    @Test
    void createToken_multipleRoles_tokenIsValid() {
        // when
        final String token = sut.createToken("user", List.of(Role.ROLE_STUDENT, Role.ROLE_TEACHER));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("user");
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void resolveToken_noAuthorizationHeader_returnsNull() {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_emptyHeader_returnsNull() {
        // given
        when(request.getHeader("Authorization")).thenReturn("");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void validateToken_emptyString_throwsValidationException() {
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(""));
    }

    @Test
    void validateToken_expiredToken_throwsValidationException() {
        // given - create provider with 0ms validity to force expired token
        final JwtTokenProvider expiredProvider = new JwtTokenProvider(myUserDetails, 0, "test-secret-key");
        final String token = expiredProvider.createToken("user", List.of(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void getUsername_tokenWithDifferentSecret_throwsException() {
        // given
        final JwtTokenProvider otherProvider = new JwtTokenProvider(myUserDetails, 3600000, "different-secret");
        final String token = otherProvider.createToken("user", List.of(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token));
    }
}
