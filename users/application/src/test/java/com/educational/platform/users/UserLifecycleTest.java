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

@ExtendWith(MockitoExtension.class)
public class UserLifecycleTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void toDTO_returnsCorrectValues() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser(RoleDTO.ROLE_STUDENT);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("john");
        assertThat(dto.email()).isEqualTo("john@test.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toDTO_teacherRole_returnsTeacherRole() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser(RoleDTO.ROLE_TEACHER);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void constructor_encodesPassword() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded-value");
        final User user = createUser(RoleDTO.ROLE_STUDENT);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getPassword()).isEqualTo("encoded-value");
    }

    @Test
    void toUserDetails_containsCorrectUsername() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser(RoleDTO.ROLE_STUDENT);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getUsername()).isEqualTo("john");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void toUserDetails_hasCorrectAuthority() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        final User user = createUser(RoleDTO.ROLE_TEACHER);

        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_TEACHER");
    }

    private User createUser(RoleDTO role) {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("john")
                .email("john@test.com")
                .password("password")
                .role(role)
                .build();
        return new User(command, passwordEncoder);
    }
}
