package com.educational.platform.users.security;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MyUserDetails sut;

    @Test
    void loadUserByUsername_existingUser_userDetails() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(command, new BCryptPasswordEncoder());
        when(userRepository.findByUsername("username")).thenReturn(Optional.of(user));

        // when
        final UserDetails userDetails = sut.loadUserByUsername("username");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("username");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_STUDENT");
    }

    @Test
    void loadUserByUsername_unknownUser_usernameNotFoundException() {
        // given
        when(userRepository.findByUsername("username")).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable load = () -> sut.loadUserByUsername("username");

        // then
        assertThatExceptionOfType(UsernameNotFoundException.class).isThrownBy(load);
    }
}
