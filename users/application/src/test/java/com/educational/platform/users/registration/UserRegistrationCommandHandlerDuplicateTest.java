package com.educational.platform.users.registration;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.security.JwtTokenProvider;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationCommandHandlerDuplicateTest {

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
    void handle_duplicateUsername_unprocessableEntityException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("user@gmail.com")
                .username("existing")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("existing")).thenReturn(true);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(handle);
        verify(repository, never()).save(any());
    }

    @Test
    void handle_invalidEmail_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("not-an-email")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_usernameTooShort_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("user@gmail.com")
                .username("ab")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }
}
