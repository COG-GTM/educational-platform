package com.educational.platform.users.security;

import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsEdgeCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MyUserDetails sut;

    @Test
    void loadUserByUsername_userNotFound_usernameNotFoundException() {
        // given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // when
        final var load = new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                sut.loadUserByUsername("nonexistent");
            }
        };

        // then
        assertThatExceptionOfType(UsernameNotFoundException.class).isThrownBy(load);
    }

    @Test
    void loadUserByUsername_existingUser_userDetailsReturned() {
        // given
        final User user = org.mockito.Mockito.mock(User.class);
        final var userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encoded")
                .authorities("ROLE_STUDENT")
                .build();
        when(user.toUserDetails()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // when
        final var result = sut.loadUserByUsername("testuser");

        // then
        org.assertj.core.api.Assertions.assertThat(result.getUsername()).isEqualTo("testuser");
    }
}
