package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void create_validCommand_fieldsMappedRoleResolvedAndPasswordEncoded() {
        // given - the constructor maps username/email verbatim, encodes the raw password through the
        // injected encoder (so the plaintext is never stored), and resolves the RoleDTO to its domain Role
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@test.com")
                .password("raw-password")
                .build();
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");

        // when
        final User user = new User(command, passwordEncoder);

        // then
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@test.com")
                .hasFieldOrPropertyWithValue("password", "encoded-password")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_TEACHER);
        verify(passwordEncoder).encode("raw-password");
    }

    @Test
    void toDTO_studentUser_usernameEmailAndRoleMappedToDto() {
        // given - toDTO() builds the payload the registration handler emits as a
        // UserCreatedIntegrationEvent, so the username/email/role mapping is a cross-module contract
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("student")
                .email("student@test.com")
                .password("raw-password")
                .build();
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "student")
                .hasFieldOrPropertyWithValue("email", "student@test.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toUserDetails_mapsUsernameEncodedPasswordAndSingleAuthority() {
        // given - the sign-in path projects the user into Spring Security's UserDetails: the username and
        // already-encoded password are carried over and the domain Role becomes the sole granted authority
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@test.com")
                .password("raw-password")
                .build();
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("teacher");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TEACHER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }
}
