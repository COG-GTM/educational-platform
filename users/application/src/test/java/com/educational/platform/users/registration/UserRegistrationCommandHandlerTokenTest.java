package com.educational.platform.users.registration;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationCommandHandlerTokenTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private UserRegistrationCommandHandler sut;

    @BeforeEach
    void setUp() {
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new UserRegistrationCommandHandler(transactionTemplate, passwordEncoder, jwtTokenProvider, repository, eventPublisher, validator);
    }

    @Test
    void handle_studentRole_tokenCreatedWithStudentRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("student@test.com")
                .username("student")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("student")).thenReturn(false);
        when(jwtTokenProvider.createToken(eq("student"), eq(Collections.singletonList(Role.ROLE_STUDENT)))).thenReturn("student-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("student-token");
        verify(jwtTokenProvider).createToken("student", Collections.singletonList(Role.ROLE_STUDENT));
    }

    @Test
    void handle_teacherRole_tokenCreatedWithTeacherRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@test.com")
                .username("teacher")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher")).thenReturn(false);
        when(jwtTokenProvider.createToken(eq("teacher"), eq(Collections.singletonList(Role.ROLE_TEACHER)))).thenReturn("teacher-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("teacher-token");
        verify(jwtTokenProvider).createToken("teacher", Collections.singletonList(Role.ROLE_TEACHER));
    }

    @Test
    void handle_validCommand_passwordEncoderCalled() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("user@test.com")
                .username("user")
                .password("rawpassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("user")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        verify(passwordEncoder).encode("rawpassword");
    }
}
