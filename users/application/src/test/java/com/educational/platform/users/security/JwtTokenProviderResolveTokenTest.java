package com.educational.platform.users.security;

import com.educational.platform.users.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderResolveTokenTest {

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
    void resolveToken_emptyHeader_returnsNull() {
        // given
        when(request.getHeader("Authorization")).thenReturn("");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isNull();
    }

    @Test
    void resolveToken_bearerOnly_returnsEmptyString() {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // when
        final String result = sut.resolveToken(request);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void createToken_multipleRoles_roundtripsUsername() {
        // given
        final String token = sut.createToken("admin", List.of(Role.ROLE_ADMIN, Role.ROLE_TEACHER));

        // then
        assertThat(sut.getUsername(token)).isEqualTo("admin");
        assertThat(sut.validateToken(token)).isTrue();
    }

    @Test
    void createToken_emptyRoleList_roundtripsUsername() {
        // given
        final String token = sut.createToken("noRoleUser", List.of());

        // then
        assertThat(sut.getUsername(token)).isEqualTo("noRoleUser");
        assertThat(sut.validateToken(token)).isTrue();
    }
}
