package com.educational.platform.users.security;

import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyUserDetailsTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MyUserDetails sut;

    @Test
    void loadUserByUsername_existingUser_returnsUserProjectedToUserDetails() {
        // given - the sign-in entry point looks the user up by username and delegates the Spring Security
        // projection to the domain User, so the returned details must be exactly what User.toUserDetails() yields
        final User user = mock(User.class);
        final UserDetails userDetails = mock(UserDetails.class);
        when(userRepository.findByUsername("teacher")).thenReturn(Optional.of(user));
        when(user.toUserDetails()).thenReturn(userDetails);

        // when
        final UserDetails result = sut.loadUserByUsername("teacher");

        // then
        assertThat(result).isSameAs(userDetails);
    }

    @Test
    void loadUserByUsername_unknownUser_usernameNotFoundExceptionIdentifiesUsername() {
        // given - an unresolved username must fail authentication with a diagnostic message that names the user
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername("ghost"))
                .withMessageContaining("ghost");
    }
}
