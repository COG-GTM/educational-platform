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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationCommandHandlerEdgeCasesTest {

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
    void handle_validCommand_returnsJwtToken() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@example.com")
                .username("testuser")
                .password("Passw0rd!")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("testuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("jwt-token-value");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("jwt-token-value");
    }

    @Test
    void handle_nullEmail_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email(null)
                .username("user")
                .password("Passw0rd!")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_nullUsername_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@example.com")
                .username(null)
                .password("Passw0rd!")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_duplicateUsername_unprocessableEntityExceptionContainsUsername() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@example.com")
                .username("existinguser")
                .password("Passw0rd!")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("existinguser")).thenReturn(true);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("existinguser");
    }
}
