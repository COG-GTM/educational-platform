package com.educational.platform.users.registration;

import com.educational.platform.users.RoleDTO;
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

@ExtendWith(MockitoExtension.class)
public class UserRegistrationCommandHandlerEventTest {

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
    void handle_validCommand_publishesUserCreatedIntegrationEvent() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("user@example.com")
                .username("newuser")
                .password("Passw0rd!")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("newuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> captor =
                ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("newuser");
        assertThat(captor.getValue().email()).isEqualTo("user@example.com");
    }

    @Test
    void handle_teacherRole_publishesEventWithCorrectUsername() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@example.com")
                .username("teacheruser")
                .password("SecureP4ss")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacheruser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("teacher-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("teacher-token");
        final ArgumentCaptor<UserCreatedIntegrationEvent> captor =
                ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("teacheruser");
        assertThat(captor.getValue().email()).isEqualTo("teacher@example.com");
    }
}
