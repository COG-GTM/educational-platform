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
 * Tests {@link User} creation and DTO/UserDetails mapping for the TEACHER role.
 * Complements {@link UserTest} which covers the STUDENT role.
 */
@ExtendWith(MockitoExtension.class)
public class UserTeacherRoleTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void toDTO_teacherRole_mapsCorrectly() {
        // given
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher1")
                .email("teacher@school.com")
                .password("secret")
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("teacher1");
        assertThat(dto.email()).isEqualTo("teacher@school.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void toUserDetails_teacherRole_hasTeacherAuthority() {
        // given
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher1")
                .email("teacher@school.com")
                .password("secret")
                .build();
        final User user = new User(command, passwordEncoder);

        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("teacher1");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-secret");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_TEACHER");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }
}
