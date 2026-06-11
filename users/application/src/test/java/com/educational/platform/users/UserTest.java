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

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .build();
        user = new User(command, passwordEncoder);
    }

    @Test
    void toDTO_mapsUsernameEmailAndRole() {
        // when
        final UserDTO dto = user.toDTO();

        // then
        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.email()).isEqualTo("email@gmail.com");
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toUserDetails_buildsUserDetailsWithEncodedPasswordAndRole() {
        // when
        final UserDetails userDetails = user.toUserDetails();

        // then
        assertThat(userDetails.getUsername()).isEqualTo("username");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
    }
}
