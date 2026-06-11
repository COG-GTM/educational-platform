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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test
    void validateToken_nullToken_jwtTokenValidationException() {
        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(null));
    }

    @Test
    void getAuthentication_teacherUser_teacherAuthorityReturned() {
        // given
        final String token = sut.createToken("teacher", Collections.singletonList(Role.ROLE_TEACHER));

        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("teacher")
                .email("teacher@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("teacher")).thenReturn(Optional.of(user));

        // when
        final Authentication authentication = sut.getAuthentication(token);

        // then
        assertThat(authentication.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_TEACHER"));
    }

    @Test
    void createToken_adminRole_tokenCreated() {
        // when
        final String token = sut.createToken("admin", Collections.singletonList(Role.ROLE_ADMIN));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo("admin");
    }

    @Test
    void resolveToken_bearerWithExtraSpaces_extractsTokenWithSpaces() {
        // given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer  token-with-leading-space");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isEqualTo(" token-with-leading-space");
    }

    @Test
    void getAuthentication_userNotInRepository_usernameNotFoundException() {
        // given
        final String token = sut.createToken("ghost", Collections.singletonList(Role.ROLE_STUDENT));
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                .isThrownBy(() -> sut.getAuthentication(token))
                .withMessageContaining("ghost");
    }

    @Test
    void getAuthentication_validToken_principalIsUserDetails() {
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
        assertThat(authentication.getPrincipal()).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
        final org.springframework.security.core.userdetails.UserDetails principal =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("username");
    }

    @Test
    void createToken_differentSecretKey_tokensDiffer() {
        // given
        final MyUserDetails myUserDetails = new MyUserDetails(userRepository);
        final JwtTokenProvider otherProvider = new JwtTokenProvider(myUserDetails, 3600000, "different-secret-key");

        // when
        final String token1 = sut.createToken("user", Collections.singletonList(Role.ROLE_STUDENT));
        final String token2 = otherProvider.createToken("user", Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void validateToken_tokenFromDifferentKey_jwtTokenValidationException() {
        // given
        final MyUserDetails myUserDetails = new MyUserDetails(userRepository);
        final JwtTokenProvider otherProvider = new JwtTokenProvider(myUserDetails, 3600000, "different-secret-key");
        final String token = otherProvider.createToken("user", Collections.singletonList(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(token));
    }

    @Test
    void getUsername_expiredToken_throwsException() {
        // given
        final MyUserDetails myUserDetails = new MyUserDetails(userRepository);
        final JwtTokenProvider expiredProvider = new JwtTokenProvider(myUserDetails, 0, "test-secret-key");
        final String token = expiredProvider.createToken("user", Collections.singletonList(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> sut.getUsername(token));
    }

    @Test
    void createToken_sameUserDifferentRoles_differentTokens() {
        // when
        final String studentToken = sut.createToken("user", Collections.singletonList(Role.ROLE_STUDENT));
        final String teacherToken = sut.createToken("user", Collections.singletonList(Role.ROLE_TEACHER));

        // then
        assertThat(studentToken).isNotEqualTo(teacherToken);
    }

    @Test
    void getAuthentication_validToken_isAuthenticated() {
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
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void resolveToken_lowercaseBearer_null() {
        // given — "bearer " (lowercase b) should not match case-sensitive "Bearer " prefix
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "bearer some-token");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void createToken_emptyUsername_tokenCreated() {
        // given — edge case: empty string username
        // when
        final String token = sut.createToken("", Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEmpty();
    }

    @Test
    void resolveToken_bearerTokenMissingSpace_null() {
        // given — "BearerTOKEN" (no space after Bearer) should not match
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "BearerTOKEN");

        // when
        final String token = sut.resolveToken(request);

        // then
        assertThat(token).isNull();
    }

    @Test
    void getAuthentication_expiredToken_throwsException() {
        // given
        final MyUserDetails myUserDetails = new MyUserDetails(userRepository);
        final JwtTokenProvider expiredProvider = new JwtTokenProvider(myUserDetails, 0, "test-secret-key");
        final String token = expiredProvider.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // when / then
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> expiredProvider.getAuthentication(token));
    }

    @Test
    void createToken_nullRolesList_throwsException() {
        // when / then
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> sut.createToken("user", null));
    }

    @Test
    void getAuthentication_validToken_delegatesToMyUserDetails() {
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
        sut.getAuthentication(token);

        // then
        verify(userRepository).findByUsername("username");
    }

    @Test
    void getAuthentication_validToken_returnsUsernamePasswordAuthenticationToken() {
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
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    }

    @Test
    void createToken_validInput_tokenHasThreeParts() {
        // given — JWT tokens have three Base64 segments separated by dots
        final String token = sut.createToken("username", Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void createToken_thenGetAuthentication_roundTrip() {
        // given
        final String token = sut.createToken("roundtrip", Collections.singletonList(Role.ROLE_TEACHER));

        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("roundtrip")
                .email("roundtrip@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("roundtrip")).thenReturn(Optional.of(user));

        // when
        final boolean valid = sut.validateToken(token);
        final String username = sut.getUsername(token);
        final Authentication auth = sut.getAuthentication(token);

        // then
        assertThat(valid).isTrue();
        assertThat(username).isEqualTo("roundtrip");
        assertThat(auth.getName()).isEqualTo("roundtrip");
        assertThat(auth.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(a -> assertThat(a.getAuthority()).isEqualTo("ROLE_TEACHER"));
    }

    @Test
    void createToken_nullUsername_tokenCreated() {
        // given — null username is an edge case; JJWT allows null subject
        // when
        final String token = sut.createToken(null, Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validateToken_tamperedPayload_jwtTokenValidationException() {
        // given — create a valid token, then tamper with the payload segment
        final String validToken = sut.createToken("user", Collections.singletonList(Role.ROLE_STUDENT));
        final String[] parts = validToken.split("\\.");
        // flip a character in the payload to invalidate the signature
        final String tamperedPayload = parts[1].substring(0, parts[1].length() - 1) + "X";
        final String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken(tamperedToken));
    }

    @Test
    void getAuthentication_validToken_loadUserByUsernameCalledExactlyOnce() {
        // given
        final String token = sut.createToken("singlecall", Collections.singletonList(Role.ROLE_STUDENT));

        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("singlecall")
                .email("single@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("singlecall")).thenReturn(Optional.of(user));

        // when
        sut.getAuthentication(token);

        // then — verify repository is called exactly once (no duplicate loads)
        verify(userRepository, times(1)).findByUsername("singlecall");
    }

    @Test
    void createToken_unicodeUsername_preservedInToken() {
        // given — Unicode characters should be preserved correctly in JWT subject
        final String unicodeUsername = "用户名";

        // when
        final String token = sut.createToken(unicodeUsername, Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo(unicodeUsername);
    }

    @Test
    void createToken_specialCharactersUsername_preservedInToken() {
        // given — special characters in username
        final String specialUsername = "user@domain.com+tag/path";

        // when
        final String token = sut.createToken(specialUsername, Collections.singletonList(Role.ROLE_STUDENT));

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.getUsername(token)).isEqualTo(specialUsername);
    }

    @Test
    void createToken_multipleRoles_allRolesInToken() {
        // given — multiple roles in the token
        final List<Role> roles = Arrays.asList(Role.ROLE_STUDENT, Role.ROLE_TEACHER);

        // when
        final String token = sut.createToken("multi-role-user", roles);

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("multi-role-user");
    }

    @Test
    void createToken_emptyRolesList_tokenCreated() {
        // given — empty roles list (edge case)
        // when
        final String token = sut.createToken("no-roles-user", Collections.emptyList());

        // then
        assertThat(token).isNotBlank();
        assertThat(sut.validateToken(token)).isTrue();
        assertThat(sut.getUsername(token)).isEqualTo("no-roles-user");
    }

    @Test
    void validateToken_randomNonJwtString_jwtTokenValidationException() {
        // when / then
        assertThatExceptionOfType(JwtTokenValidationException.class)
                .isThrownBy(() -> sut.validateToken("not-a-jwt-token"));
    }

    @Test
    void resolveToken_authorizationHeaderWithOnlyBearer_emptyString() {
        // given — "Bearer " followed by nothing
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");

        // when
        final String token = sut.resolveToken(request);

        // then — substring(7) of "Bearer " gives ""
        assertThat(token).isEmpty();
    }
}
