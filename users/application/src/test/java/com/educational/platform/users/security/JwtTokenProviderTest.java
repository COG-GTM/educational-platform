package com.educational.platform.users.security;

import com.educational.platform.security.JwtTokenValidationException;
import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider sut;

    @BeforeEach
    void setUp() {
        final MyUserDetails myUserDetails = new MyUserDetails(userRepository);
        sut = new JwtTokenProvider(myUserDetails, 3600000, "test-secret-key");
    }

    @Test
    void createToken_validInput_tokenCreated() {
        // when
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    void getUsername_validToken_usernameExtracted() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final String username = sut.getUsername(token);

        // then
        assertThat(username).isEqualTo("username");
    }

    @Test
    void validateToken_validToken_true() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when
        final boolean result = sut.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void validateToken_invalidToken_jwtTokenValidationException() {
        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken("invalid-token"));
    }

    @Test
    void getAuthentication_validToken_authenticationReturned() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("username");
    }

    @Test
    void resolveToken_bearerTokenPresent_tokenExtracted() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isEqualTo("test-token");
    }

    @Test
    void resolveToken_noAuthorizationHeader_null() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void resolveToken_nonBearerToken_null() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void createToken_teacherRole_tokenContainsCorrectUsername() {
        // given
        final String token = sut.createToken("teacher", Collections.singletonList(Role.ROLE_TEACHER));

        // when
        final String username = sut.getUsername(token);

        // then
        assertThat(username).isEqualTo("teacher");
    }

    @Test
    void createToken_emptyRoles_tokenCreated() {
        // when
        final String token = sut.createToken("username", Collections.emptyList());

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("username");
    }

    @Test
    void createToken_multipleRoles_tokenCreated() {
        // given
        final List<Role> roles = Arrays.asList(Role.ROLE_STUDENT, Role.ROLE_TEACHER);

        // when
        final String token = sut.createToken("multi", roles);

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("multi");
    }

    @Test
    void resolveToken_bearerPrefixOnly_emptyStringReturned() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isEmpty();
    }

    @Test
    void validateToken_emptyString_jwtTokenValidationException() {
        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(""));
    }

    @Test
    void validateToken_validToken_exceptionMessageContainsDetail() {
        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken("invalid-token"))
                .withMessageContaining("Expired or invalid JWT token");
    }

    @Test
    void getAuthentication_validToken_authoritiesPreserved() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_STUDENT"));
    }

    @Test
    void getUsername_invalidToken_throwsException() {
        // when / then
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> sut.getUsername("not-a-valid-token"));
    }

    @Test
    void createToken_thenValidate_roundTrip() {
        // given
        final String token = sut.createToken("roundtrip-user", Collections.singletonList(Role.ROLE_TEACHER));

        // when
        final boolean valid = sut.validateToken(token);
        final String username = sut.getUsername(token);

        // then
        assertThat(valid).isTrue();
        assertThat(username).isEqualTo("roundtrip-user");
    }

    @Test
    void resolveToken_emptyAuthorizationHeader_null() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void createToken_expiredToken_jwtTokenValidationException() {
        // given
        final MyUserDetails myUserDetails = new MyUserDetails(userRepository);
        final JwtTokenProvider expiredProvider = new JwtTokenProvider(myUserDetails, 0, "test-secret-key");
        final String token = expiredProvider.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> expiredProvider.validateToken(token));
    }

    @Test
    void getAuthentication_validToken_credentialsEmpty() {
        // given
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getCredentials()).isEqualTo("");
    }
}
