package com.educational.platform.users.registration;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
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

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationCommandHandlerPasswordEncodingTest {

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
    void handle_validCommand_passwordEncoderCalledWithRawPassword() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@gmail.com")
                .username("testuser")
                .password("rawpassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded-password");

        // when
        sut.handle(command);

        // then
        verify(passwordEncoder).encode("rawpassword");
    }

    @Test
    void handle_validCommand_encodedPasswordStoredInUser() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@gmail.com")
                .username("testuser")
                .password("rawpassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded-password");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("password", "encoded-password");
    }

    @Test
    void handle_nullRole_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@gmail.com")
                .username("testuser")
                .password("rawpassword")
                .role(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(command));
    }

    @Test
    void handle_nullPassword_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("test@gmail.com")
                .username("testuser")
                .password(null)
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(command));
    }

    @Test
    void handle_nullEmail_constraintViolationException() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email(null)
                .username("testuser")
                .password("rawpassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(command));
    }
}
