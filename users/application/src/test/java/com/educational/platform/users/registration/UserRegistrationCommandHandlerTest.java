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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessage(UserRegistrationCommandHandler.REGISTRATION_REJECTED_MESSAGE);
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(UserCreatedIntegrationEvent.class));
    }

    @Test
    void handle_usernameInsertedConcurrently_unprocessableEntityException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false, true);
        when(repository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("custom_user_username_uk"));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessage(UserRegistrationCommandHandler.REGISTRATION_REJECTED_MESSAGE);
        verify(eventPublisher, never()).publishEvent(any(UserCreatedIntegrationEvent.class));
    }

    @Test
    void handle_unrelatedIntegrityViolation_dataIntegrityViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final DataIntegrityViolationException cause = new DataIntegrityViolationException("value too long for column email");
        when(repository.existsByUsername("username")).thenReturn(false);
        when(repository.save(any(User.class))).thenThrow(cause);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(handle)
                .isSameAs(cause);
        verify(eventPublisher, never()).publishEvent(any(UserCreatedIntegrationEvent.class));
    }

    @Test
    void handle_usernameInsertedConcurrently_transactionRolledBack() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false, true);
        when(repository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("custom_user_username_uk"));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(handle);
        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_saveFailsWithUnexpectedException_exceptionPropagated() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        final IllegalStateException failure = new IllegalStateException("datasource unavailable");
        when(repository.save(any(User.class))).thenThrow(failure);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(handle)
                .isSameAs(failure);
        verify(transactionManager).rollback(any());
        verify(eventPublisher, never()).publishEvent(any(UserCreatedIntegrationEvent.class));
    }

    @Test
    void handle_validCommand_tokenReturnedAfterCommit() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken("username", Collections.singletonList(Role.ROLE_STUDENT))).thenReturn("token");

        // when
        final String result = sut.handle(userRegistrationCommand);

        // then
        assertThat(result).isEqualTo("token");
        verify(transactionManager).commit(any());
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void handle_validCommand_eventPublishedAndTokenIssuedOnlyAfterCommit() {
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
        final InOrder inOrder = inOrder(repository, transactionManager, eventPublisher, jwtTokenProvider);
        inOrder.verify(repository).save(any(User.class));
        inOrder.verify(transactionManager).commit(any());
        inOrder.verify(eventPublisher).publishEvent(any(UserCreatedIntegrationEvent.class));
        inOrder.verify(jwtTokenProvider).createToken(any(), any());
    }

    @Test
    void handle_usernameLongerThanColumn_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("u".repeat(101))
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(repository, never()).save(any());
    }

    @Test
    void handle_usernameAtMaxLength_userCreated() {
        // given
        final String username = "u".repeat(100);
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username(username)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername(username)).thenReturn(false);

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(repository).save(any(User.class));
    }

    @Test
    void handle_emailLongerThanColumn_constraintViolationException() {
        // given
        final String email = "e".repeat(56) + "@" + "d".repeat(40) + ".com";
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email(email)
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThat(email).hasSize(101);
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
        verify(repository, never()).existsByUsername(any());
        verify(repository, never()).save(any());
    }

    @Test
    void handle_emailAtMaxLength_userCreated() {
        // given
        final String email = "e".repeat(55) + "@" + "d".repeat(40) + ".com";
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email(email)
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);

        // when
        sut.handle(userRegistrationCommand);

        // then
        assertThat(email).hasSize(100);
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue()).hasFieldOrPropertyWithValue("email", email);
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
    }
}
