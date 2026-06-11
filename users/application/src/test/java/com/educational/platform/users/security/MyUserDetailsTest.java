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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test
    void loadUserByUsername_existingUser_accountFlagsAllTrue() {
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
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_existingUser_delegatesToRepository() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("delegate-user")
                .email("delegate@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("delegate-user")).thenReturn(Optional.of(user));

        // when
        sut.loadUserByUsername("delegate-user");

        // then
        verify(userRepository).findByUsername("delegate-user");
    }

    @Test
    void loadUserByUsername_existingUser_returnsUserDetailsWithCorrectUsername() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("specificuser")
                .email("specific@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("specificuser")).thenReturn(Optional.of(user));

        // when
        final UserDetails userDetails = sut.loadUserByUsername("specificuser");

        // then — verify username in returned UserDetails matches exactly
        assertThat(userDetails.getUsername()).isEqualTo("specificuser");
        assertThat(userDetails.getUsername()).isNotEqualTo("different-user");
    }

    @Test
    void loadUserByUsername_nonExistingUser_repositoryQueried() {
        // given
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // when
        try {
            sut.loadUserByUsername("ghost");
        } catch (UsernameNotFoundException ignored) {
        }

        // then
        verify(userRepository).findByUsername("ghost");
    }

    @Test
    void myUserDetails_implementsUserDetailsService() {
        // then
        assertThat(sut).isInstanceOf(org.springframework.security.core.userdetails.UserDetailsService.class);
    }

    @Test
    void loadUserByUsername_null_usernameNotFoundException() {
        // given
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // when / then
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> sut.loadUserByUsername(null)))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_existingUser_repositoryQueriedExactlyOnce() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("single-query")
                .email("single@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("single-query")).thenReturn(Optional.of(user));

        // when
        sut.loadUserByUsername("single-query");

        // then
        verify(userRepository, times(1)).findByUsername("single-query");
    }

    @Test
    void loadUserByUsername_emptyString_usernameNotFoundException() {
        // given
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> sut.loadUserByUsername(""))
                .withMessageContaining("");
    }

    @Test
    void loadUserByUsername_existingUser_userDetailsAuthoritiesAreNotEmpty() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("authcheck")
                .email("authcheck@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("authcheck")).thenReturn(Optional.of(user));

        // when
        final UserDetails userDetails = sut.loadUserByUsername("authcheck");

        // then
        assertThat(userDetails.getAuthorities()).isNotEmpty();
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_existingUser_userDetailsIsEnabled() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username("enabled-check")
                .email("enabled@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        final User user = new User(command, passwordEncoder);
        when(userRepository.findByUsername("enabled-check")).thenReturn(Optional.of(user));

        // when
        final UserDetails userDetails = sut.loadUserByUsername("enabled-check");

        // then — all UserDetails returned by our service should be fully active
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }
}
