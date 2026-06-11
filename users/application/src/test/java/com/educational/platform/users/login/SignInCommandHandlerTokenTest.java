package com.educational.platform.users.login;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SignInCommandHandlerTokenTest {

    @Mock
    private UserRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SignInCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new SignInCommandHandler(jwtTokenProvider, repository, validator, authenticationManager);
    }

    @Test
    void handle_studentUser_createsTokenWithStudentRole() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("student-user")
                .password("password")
                .build();
        final UserRegistrationCommand regCommand = UserRegistrationCommand.builder()
                .email("student@test.com")
                .username("student-user")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(regCommand, passwordEncoder);
        when(repository.findByUsername("student-user")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("student-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("student-token");
        verify(jwtTokenProvider).createToken(eq("student-user"), eq(List.of(Role.ROLE_STUDENT)));
    }

    @Test
    void handle_teacherUser_createsTokenWithTeacherRole() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("teacher-user")
                .password("password")
                .build();
        final UserRegistrationCommand regCommand = UserRegistrationCommand.builder()
                .email("teacher@test.com")
                .username("teacher-user")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User user = new User(regCommand, passwordEncoder);
        when(repository.findByUsername("teacher-user")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("teacher-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("teacher-token");
        verify(jwtTokenProvider).createToken(eq("teacher-user"), eq(List.of(Role.ROLE_TEACHER)));
    }

    @Test
    void handle_validCommand_authenticatesWithCorrectCredentials() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("myuser")
                .password("mypassword")
                .build();
        final UserRegistrationCommand regCommand = UserRegistrationCommand.builder()
                .email("user@test.com")
                .username("myuser")
                .password("mypassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = new User(regCommand, passwordEncoder);
        when(repository.findByUsername("myuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<org.springframework.security.authentication.UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("myuser");
        assertThat(captor.getValue().getCredentials()).isEqualTo("mypassword");
    }
}
