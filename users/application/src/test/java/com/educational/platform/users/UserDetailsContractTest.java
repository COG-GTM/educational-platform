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
public class UserDetailsContractTest {

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
    void toUserDetails_accountIsNotExpired() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.isAccountNonExpired()).isTrue();
    }

    @Test
    void toUserDetails_accountIsNotLocked() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void toUserDetails_credentialsAreNotExpired() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void toUserDetails_accountIsEnabled() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void toUserDetails_teacherRole_hasTeacherAuthority() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .role(RoleDTO.ROLE_TEACHER)
                .username("teacher")
                .email("teacher@example.com")
                .password("password")
                .build();
        final User teacher = new User(command, passwordEncoder);

        // when
        final UserDetails details = teacher.toUserDetails();

        // then
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TEACHER");
    }

    @Test
    void toUserDetails_studentRole_singleAuthority() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getAuthorities()).hasSize(1);
    }

    @Test
    void toUserDetails_passwordIsEncodedValue_notRawPassword() {
        // when
        final UserDetails details = user.toUserDetails();

        // then
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getPassword()).isNotEqualTo("password");
    }
}
