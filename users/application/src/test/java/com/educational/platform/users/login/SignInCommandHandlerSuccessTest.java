package com.educational.platform.users.login;

import com.educational.platform.users.*;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignInCommandHandlerSuccessTest {

    @Mock
    private UserRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    private SignInCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new SignInCommandHandler(jwtTokenProvider, repository, validator, authenticationManager);
    }

    @Test
    void handle_validCredentials_jwtTokenReturned() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password("validpassword")
                .build();

        final UserDTO userDTO = UserDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = org.mockito.Mockito.mock(User.class);
        when(user.toDTO()).thenReturn(userDTO);
        when(repository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(eq("testuser"), any())).thenReturn("generated-jwt-token");

        // when
        final String result = sut.handle(command);

        // then
        assertThat(result).isEqualTo("generated-jwt-token");
    }

    @Test
    void handle_validCredentials_authenticationManagerCalled() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("testuser")
                .password("validpassword")
                .build();

        final UserDTO userDTO = UserDTO.builder()
                .username("testuser")
                .email("test@example.com")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User user = org.mockito.Mockito.mock(User.class);
        when(user.toDTO()).thenReturn(userDTO);
        when(repository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(eq("testuser"), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void handle_teacherCredentials_tokenCreatedWithTeacherRole() {
        // given
        final SignInCommand command = SignInCommand.builder()
                .username("teacher")
                .password("validpassword")
                .build();

        final UserDTO userDTO = UserDTO.builder()
                .username("teacher")
                .email("teacher@example.com")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User user = org.mockito.Mockito.mock(User.class);
        when(user.toDTO()).thenReturn(userDTO);
        when(repository.findByUsername("teacher")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createToken(eq("teacher"), any())).thenReturn("teacher-token");

        // when
        final String result = sut.handle(command);

        // then
        assertThat(result).isEqualTo("teacher-token");
        verify(jwtTokenProvider).createToken(eq("teacher"), any());
    }
}
