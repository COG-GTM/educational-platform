package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link User#toUserDetails()} correctly maps username and password fields.
 */
@ExtendWith(MockitoExtension.class)
public class UserToUserDetailsFieldsTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void toUserDetails_returnsCorrectUsername() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser("testuser", "test@example.com", "password", RoleDTO.ROLE_STUDENT);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getUsername()).isEqualTo("testuser");
    }

    @Test
    void toUserDetails_returnsEncodedPassword() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded-pw-123");
        final User user = createUser("testuser", "test@example.com", "password", RoleDTO.ROLE_STUDENT);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getPassword()).isEqualTo("encoded-pw-123");
    }

    @Test
    void toUserDetails_teacherRole_hasRoleTeacherAuthority() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser("teacher", "teacher@example.com", "password", RoleDTO.ROLE_TEACHER);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    void toUserDetails_studentRole_hasRoleStudentAuthority() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser("student", "student@example.com", "password", RoleDTO.ROLE_STUDENT);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void toDTO_returnsCorrectFields() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser("testuser", "test@example.com", "password", RoleDTO.ROLE_STUDENT);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("testuser");
        assertThat(dto.email()).isEqualTo("test@example.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    private User createUser(String username, String email, String password, RoleDTO role) {
        return new User(
                UserRegistrationCommand.builder()
                        .username(username)
                        .email(email)
                        .password(password)
                        .role(role)
                        .build(),
                passwordEncoder
        );
    }
}
