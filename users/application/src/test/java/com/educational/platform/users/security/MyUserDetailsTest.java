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
import static org.mockito.ArgumentMatchers.anyString;
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
    }

    @Test
    void loadUserByUsername_existingUser_userDetailsReturned() {
        // given
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
        final UserDetails userDetails = sut.loadUserByUsername("username");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("username");
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_STUDENT"));
    }

    @Test
    void loadUserByUsername_nonExistingUser_usernameNotFoundException() {
        // given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername("unknown"));
    }

    @Test
    void loadUserByUsername_nonExistingUser_exceptionContainsUsername() {
        // given
        when(userRepository.findByUsername("missinguser")).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername("missinguser"))
                .withMessageContaining("missinguser");
    }

    @Test
    void loadUserByUsername_existingTeacher_correctAuthority() {
        // given
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
        final UserDetails userDetails = sut.loadUserByUsername("teacher");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("teacher");
        assertThat(userDetails.getAuthorities())
                .hasSize(1)
                .first()
                .satisfies(authority -> assertThat(authority.getAuthority()).isEqualTo("ROLE_TEACHER"));
    }

    @Test
    void loadUserByUsername_existingUser_passwordIsEncoded() {
        // given
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
        final UserDetails userDetails = sut.loadUserByUsername("username");

        // then
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
    }
}
