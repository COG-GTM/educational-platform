package com.educational.platform.users.security;

import com.educational.platform.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

/**
 * Verifies that the UsernameNotFoundException thrown by MyUserDetails
 * includes the username in the error message for debugging.
 */
@ExtendWith(MockitoExtension.class)
public class MyUserDetailsErrorMessageTest {

    @Mock
    private UserRepository userRepository;

    private MyUserDetails sut;

    @BeforeEach
    void setUp() {
        sut = new MyUserDetails(userRepository);
    }

    @Test
    void loadUserByUsername_missingUser_exceptionContainsUsername() {
        // given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername("unknown"))
                .withMessageContaining("unknown");
    }
}
