package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies User mapping for ROLE_TEACHER — the existing UserTest only covers ROLE_STUDENT.
 */
@ExtendWith(MockitoExtension.class)
public class UserTeacherRoleTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@example.com")
                .password("password")
                .build();
        user = new User(command, passwordEncoder);
    }

    @Test
    void toDTO_mapsTeacherRole() {
        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("teacher");
        assertThat(dto.email()).isEqualTo("teacher@example.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_teacherRole_hasTeacherAuthority() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getUsername()).isEqualTo("teacher");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_TEACHER");
    }

    @Test
    void toUserDetails_accountFlags_allEnabled() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }
}
