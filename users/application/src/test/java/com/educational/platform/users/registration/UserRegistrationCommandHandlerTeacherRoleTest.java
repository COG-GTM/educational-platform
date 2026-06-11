package com.educational.platform.users.registration;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the registration handler correctly maps the TEACHER role
 * and publishes the integration event with proper email/username.
 */
@ExtendWith(MockitoExtension.class)
public class UserRegistrationCommandHandlerTeacherRoleTest {

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
    void handle_teacherRole_savesUserWithTeacherRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@example.com")
                .username("teacher1")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher1")).thenReturn(false);

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("username", "teacher1")
                .hasFieldOrPropertyWithValue("email", "teacher@example.com")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_TEACHER);
    }

    @Test
    void handle_teacherRole_publishesEventWithCorrectEmailAndUsername() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@example.com")
                .username("teacher1")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher1")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> captor =
                ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("username", "teacher1")
                .hasFieldOrPropertyWithValue("email", "teacher@example.com");
    }
}
