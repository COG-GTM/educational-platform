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
        sut = new JwtTokenProvider(myUserDetails, 3600000, "secret-key");
    }

    @Test
    void resolveToken_nullHeader_returnsNull() {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_emptyString_returnsNull() {
        // given
        when(request.getHeader("Authorization")).thenReturn("");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_bearerOnly_returnsEmptyToken() {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void createToken_multipleRoles_tokenContainsSubject() {
        // when
        final String token = sut.createToken("admin", List.of(Role.ROLE_STUDENT, Role.ROLE_TEACHER));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("admin");
    }

    @Test
    void validateToken_emptyString_throwsValidationException() {
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(""));
    }

    @Test
    void validateToken_nullToken_throwsValidationException() {
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(null));
    }

    @Test
    void createToken_emptyRolesList_tokenIsValid() {
        // when
        final String token = sut.createToken("user", List.of());

        // then
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("user");
    }
}
