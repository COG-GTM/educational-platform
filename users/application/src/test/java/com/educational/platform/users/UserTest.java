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
class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void constructor_encodesPasswordAndMapsRole() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        final UserRegistrationCommand command = command(RoleDTO.ROLE_TEACHER);

        // when
        final User sut = new User(command, passwordEncoder);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("password", "encoded-password")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_TEACHER);
    }

    @Test
    void toDTO_mapsFields() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        final User sut = new User(command(RoleDTO.ROLE_STUDENT), passwordEncoder);

        // when
        final UserDTO dto = sut.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("role", RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toUserDetails_buildsEnabledNonExpiredUser() {
        // given
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        final User sut = new User(command(RoleDTO.ROLE_STUDENT), passwordEncoder);

        // when
        final UserDetails userDetails = sut.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("username");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    private static UserRegistrationCommand command(RoleDTO role) {
        return UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(role)
                .build();
    }
}
