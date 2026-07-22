package com.educational.platform.users.registration;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import com.educational.platform.users.security.JwtTokenProvider;
import org.assertj.core.api.ThrowableAssert;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserRegistrationCommandHandlerTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private UserRegistrationCommandHandler sut;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new UserRegistrationCommandHandler(transactionTemplate, passwordEncoder, jwtTokenProvider, repository, eventPublisher, validator);
    }

    @Test
    void handle_validCommand_userCreated() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);

        // when
        sut.handle(userRegistrationCommand);

        // then
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        final User user = argument.getValue();
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_STUDENT);

        final ArgumentCaptor<UserCreatedIntegrationEvent> eventArgument = ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final UserCreatedIntegrationEvent event = eventArgument.getValue();
        assertThat(event)
                .hasFieldOrPropertyWithValue("username", "username")
                .hasFieldOrPropertyWithValue("email", "email@gmail.com");
    }

    @Test
    void handle_validCommand_passwordEncodedBeforePersist() {
        // given
        final UserRegistrationCommand command = validCommand();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");

        // when
        sut.handle(command);

        // then
        verify(passwordEncoder).encode("password");
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue()).hasFieldOrPropertyWithValue("password", "encoded-password");
    }

    @Test
    void handle_validCommand_tokenReturnedAndJwtProviderCalled() {
        // given
        final UserRegistrationCommand command = validCommand();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken("username", java.util.Collections.singletonList(Role.ROLE_STUDENT)))
                .thenReturn("jwt-token");

        // when
        final String result = sut.handle(command);

        // then
        assertThat(result).isEqualTo("jwt-token");
        verify(jwtTokenProvider).createToken("username", java.util.Collections.singletonList(Role.ROLE_STUDENT));
    }

    @Test
    void handle_usernameAlreadyExists_unprocessableEntityException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(true);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(handle);
        verify(repository).existsByUsername("username");
        verify(repository, org.mockito.Mockito.never()).save(any(User.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_roleIsEmpty_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_usernameIsEmpty_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username(null)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_emailIsEmpty_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email(null)
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_passwordIsEmpty_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password(null)
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_usernameIsBlank_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().username(" ").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_emailIsBlank_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().email(" ").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_passwordIsBlank_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().password(" ").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_emailHasInvalidFormat_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().email("invalid-email").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_usernameIsTooShort_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().username("usr").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_usernameIsTooLong_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().username("a".repeat(256)).build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_passwordIsTooShort_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().password("short").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_passwordIsTooLong_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().password("a".repeat(31)).build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void handle_passwordContainsWhitespace_constraintViolationException() {
        // given
        final UserRegistrationCommand command = validCommandBuilder().password("pass word").build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verifyNoInteractions(repository, eventPublisher);
    }

    private static UserRegistrationCommand validCommand() {
        return validCommandBuilder().build();
    }

    private static UserRegistrationCommand.UserRegistrationCommandBuilder validCommandBuilder() {
        return UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT);
    }
}
