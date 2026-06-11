package com.educational.platform.users.registration;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.educational.platform.users.Role;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationCommandHandlerTokenTest {

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
    void handle_validCommand_returnsNonNullToken() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("jwt-token-value");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("jwt-token-value");
    }

    @Test
    void handle_teacherRole_returnsToken() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@school.com")
                .username("teacher1")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher1")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("teacher-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("teacher-token");
    }

    @Test
    void handle_validCommand_publishesUserCreatedEvent() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("event@school.com")
                .username("eventuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("eventuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final org.mockito.ArgumentCaptor<com.educational.platform.users.integration.event.UserCreatedIntegrationEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.educational.platform.users.integration.event.UserCreatedIntegrationEvent.class);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("eventuser");
        assertThat(captor.getValue().email()).isEqualTo("event@school.com");
    }

    @Test
    void handle_studentRole_createsTokenWithCorrectUsernameAndRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("student@school.com")
                .username("student1")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("student1")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        verify(jwtTokenProvider).createToken(
                eq("student1"),
                eq(Collections.singletonList(Role.ROLE_STUDENT)));
    }

    @Test
    void handle_teacherRole_createsTokenWithTeacherRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@school.com")
                .username("teacher1")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher1")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        verify(jwtTokenProvider).createToken(
                eq("teacher1"),
                eq(Collections.singletonList(Role.ROLE_TEACHER)));
    }
}
