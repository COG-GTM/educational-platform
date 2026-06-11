package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyUserDetailsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MyUserDetails sut;

    @BeforeEach
    void setUp() {
        sut = new MyUserDetails(userRepository);
        lenient().when(passwordEncoder.encode("password")).thenReturn("encoded-password");
    }

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_STUDENT)
                .username("username")
                .email("email@gmail.com")
                .password("password")
                .build();
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));

        // when
        final UserDetails result = sut.loadUserByUsername("username");

        // then
        assertThat(result.getUsername()).isEqualTo("username");
    }

    @Test
    void loadUserByUsername_missingUser_throwsUsernameNotFoundException() {
        // given
        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());

        // then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername("username"));
    }
}
