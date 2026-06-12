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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationCommandHandlerEventTest {

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
    void handle_validCommand_userCreatedIntegrationEventPublished() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("newuser@gmail.com")
                .username("newuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("newuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(eq("newuser"), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final UserCreatedIntegrationEvent event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("newuser");
        assertThat(event.email()).isEqualTo("newuser@gmail.com");
    }

    @Test
    void handle_teacherRegistration_eventContainsTeacherEmail() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@school.com")
                .username("teacher1")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher1")).thenReturn(false);
        when(jwtTokenProvider.createToken(eq("teacher1"), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> eventCaptor = ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        final UserCreatedIntegrationEvent event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo("teacher1");
        assertThat(event.email()).isEqualTo("teacher@school.com");
    }
}
