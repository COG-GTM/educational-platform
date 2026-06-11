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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

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
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(handle);
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

    @Test
    void handle_validCommand_returnsTokenFromProvider() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("expected-jwt-token");

        // when
        final String token = sut.handle(userRegistrationCommand);

        // then
        assertThat(token).isEqualTo("expected-jwt-token");
    }

    @Test
    void handle_teacherRole_userCreatedWithTeacherRole() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("teacher@gmail.com")
                .username("teacher")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        final User user = argument.getValue();
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "teacher")
                .hasFieldOrPropertyWithValue("email", "teacher@gmail.com")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_TEACHER);
    }

    @Test
    void handle_teacherRole_tokenCreatedWithTeacherRole() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("teacher@gmail.com")
                .username("teacher")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(jwtTokenProvider).createToken(eq("teacher"), eq(Collections.singletonList(Role.ROLE_TEACHER)));
    }

    @Test
    void handle_validCommand_publishesEventWithCorrectUsernameAndEmail() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("event@gmail.com")
                .username("eventuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("eventuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> eventArgument = ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final UserCreatedIntegrationEvent event = eventArgument.getValue();
        assertThat(event.username()).isEqualTo("eventuser");
        assertThat(event.email()).isEqualTo("event@gmail.com");
    }

    @Test
    void handle_usernameAlreadyExists_exceptionMessageContainsUsername() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("duplicate")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("duplicate")).thenReturn(true);

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("duplicate");
    }

    @Test
    void handle_validCommand_passwordEncoderInvoked() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("raw-password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(passwordEncoder).encode("raw-password");
    }

    @Test
    void handle_validCommand_tokenCreatedWithStudentRole() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("student")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("student")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(jwtTokenProvider).createToken(eq("student"), eq(Collections.singletonList(Role.ROLE_STUDENT)));
    }

    @Test
    void handle_validCommand_eventPublishedExactlyOnce() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("once@gmail.com")
                .username("onceuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("onceuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(eventPublisher, times(1)).publishEvent(any(UserCreatedIntegrationEvent.class));
    }

    @Test
    void handle_validCommand_userSavedExactlyOnce() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("save@gmail.com")
                .username("saveuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("saveuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(repository, times(1)).save(any(User.class));
    }

    @Test
    void handle_usernameAlreadyExists_eventNotPublished() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("existing")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("existing")).thenReturn(true);

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (UnprocessableEntityException ignored) {
        }

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_usernameAlreadyExists_userNotSaved() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("existing")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("existing")).thenReturn(true);

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (UnprocessableEntityException ignored) {
        }

        // then
        verify(repository, never()).save(any());
    }

    @Test
    void handle_validCommand_existsByUsernameCalledWithCorrectUsername() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("checkuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("checkuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(repository).existsByUsername("checkuser");
    }

    @Test
    void handle_validationFails_repositoryNeverChecked() {
        // given — null role triggers @NotNull violation
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
    }

    @Test
    void handle_validationFails_eventNotPublished() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handle_validationFails_userNotSaved() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).save(any());
    }

    @Test
    void handle_validationFails_tokenNotCreated() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_validationFails_passwordNeverEncoded() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void handle_usernameAlreadyExists_tokenNotCreated() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("taken")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("taken")).thenReturn(true);

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (UnprocessableEntityException ignored) {
        }

        // then
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_usernameAlreadyExists_passwordNeverEncoded() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("taken")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("taken")).thenReturn(true);

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (UnprocessableEntityException ignored) {
        }

        // then
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void handle_validCommand_existsByUsernameCalledBeforeSave() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("order@gmail.com")
                .username("orderuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("orderuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final var inOrder = inOrder(repository);
        inOrder.verify(repository).existsByUsername("orderuser");
        inOrder.verify(repository).save(any(User.class));
    }

    @Test
    void handle_validCommand_saveCalledBeforeEventPublished() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("order@gmail.com")
                .username("orderuser2")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("orderuser2")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final var inOrder = inOrder(repository, eventPublisher);
        inOrder.verify(repository).save(any(User.class));
        inOrder.verify(eventPublisher).publishEvent(any(UserCreatedIntegrationEvent.class));
    }

    @Test
    void handle_invalidEmail_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("not-an-email")
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
    void handle_usernameTooShort_constraintViolationException() {
        // given — username "ab" violates @Size(min = 4)
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("ab")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_invalidEmail_repositoryNeverChecked() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("not-an-email")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
    }

    @Test
    void handle_usernameTooShort_repositoryNeverChecked() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("ab")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
    }

    @Test
    void handle_blankUsername_constraintViolationException() {
        // given — whitespace-only triggers @NotBlank
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("   ")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_blankEmail_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("   ")
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
    void handle_blankPassword_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("   ")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_passwordTooShort_constraintViolationException() {
        // given — password "short" violates @ValidPassword (min length 8)
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("short")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_passwordWithWhitespace_constraintViolationException() {
        // given — password with space violates @ValidPassword whitespace rule
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("pass word1")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_allFieldsInvalid_constraintViolationException() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email(null)
                .username(null)
                .password(null)
                .role(null)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_allFieldsInvalid_noSideEffects() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email(null)
                .username(null)
                .password(null)
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void handle_validCommand_tokenIsNotBlank() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("jwt-token-value");

        // when
        final String token = sut.handle(userRegistrationCommand);

        // then
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void handle_validCommand_ordering_existsByUsernameThenSaveThenEventThenToken() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("order@gmail.com")
                .username("orderuser")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("orderuser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then — verify ordering: existsByUsername → save → publishEvent → createToken
        final var inOrderVerifier = inOrder(repository, eventPublisher, jwtTokenProvider);
        inOrderVerifier.verify(repository).existsByUsername("orderuser");
        inOrderVerifier.verify(repository).save(any(User.class));
        inOrderVerifier.verify(eventPublisher).publishEvent(any(UserCreatedIntegrationEvent.class));
        inOrderVerifier.verify(jwtTokenProvider).createToken(any(), any());
    }

    @Test
    void handle_validationFails_existsByUsernameNeverCalled() {
        // given — null role triggers @NotNull violation
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (ConstraintViolationException ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
    }

    @Test
    void handle_validationFails_shortUsername_nothingPersisted() {
        // given — username "ab" violates @Size(min=4)
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("ab")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (ConstraintViolationException ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_validationFails_invalidEmail_nothingPersisted() {
        // given — "not-an-email" violates @Email
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("not-an-email")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (ConstraintViolationException ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_usernameAlreadyExists_tokenNeverCreated() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("taken")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("taken")).thenReturn(true);

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (UnprocessableEntityException ignored) {
        }

        // then
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_teacherRole_eventPublishedWithCorrectData() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("teacher@gmail.com")
                .username("teacheruser")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacheruser")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("teacher-token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> eventArgument = ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final UserCreatedIntegrationEvent event = eventArgument.getValue();
        assertThat(event.username()).isEqualTo("teacheruser");
        assertThat(event.email()).isEqualTo("teacher@gmail.com");
    }

    @Test
    void handle_constraintViolationException_containsExpectedPropertyPath() {
        // given — null role triggers @NotNull violation
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations())
                        .anyMatch(v -> v.getPropertyPath().toString().equals("role")));
    }

    @Test
    void handle_multipleValidationFailures_allViolationsReported() {
        // given — null role + null username + null email → multiple violations
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email(null)
                .username(null)
                .password(null)
                .role(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations()).hasSizeGreaterThanOrEqualTo(3));
    }

    @Test
    void handle_validTeacherCommand_returnsNonBlankToken() {
        // given — verify teacher registration flow returns a non-blank token
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("teacher@gmail.com")
                .username("teacher")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("teacher-jwt-token");

        // when
        final String token = sut.handle(userRegistrationCommand);

        // then
        assertThat(token).isNotNull().isNotBlank();
        assertThat(token).isEqualTo("teacher-jwt-token");
    }

    @Test
    void handle_validCommand_savedUserHasEncodedPassword() {
        // given — verify the saved User contains the encoded password, not the raw one
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("raw-password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-raw-password");
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("password", "encoded-raw-password");
    }

    @Test
    void handle_validCommand_existsByUsernameCalledExactlyOnce() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(repository, times(1)).existsByUsername("username");
    }

    @Test
    void handle_validCommand_tokenCreatedExactlyOnce() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        verify(jwtTokenProvider, times(1)).createToken(any(), any());
    }

    @Test
    void handle_invalidEmail_constraintViolationContainsEmailPath() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("not-an-email")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations())
                        .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void handle_usernameTooShort_constraintViolationContainsUsernamePath() {
        // given — username "ab" violates @Size(min = 4)
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("ab")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations())
                        .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void handle_passwordTooShort_constraintViolationContainsPasswordPath() {
        // given — password "short" violates @ValidPassword (min length 8)
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("short")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations())
                        .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void handle_multipleValidationFailures_allPropertyPathsReported() {
        // given — all fields invalid: null role, short username, invalid email, short password
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("bad")
                .username("ab")
                .password("short")
                .role(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .satisfies(ex -> {
                    assertThat(ex.getConstraintViolations())
                            .anyMatch(v -> v.getPropertyPath().toString().equals("role"));
                    assertThat(ex.getConstraintViolations())
                            .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
                    assertThat(ex.getConstraintViolations())
                            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
                    assertThat(ex.getConstraintViolations())
                            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
                });
    }

    @Test
    void handle_usernameExactMinLength4_validationPasses() {
        // given — username at exact min boundary (4 chars) should pass validation
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("abcd")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("abcd")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        final String token = sut.handle(userRegistrationCommand);

        // then
        assertThat(token).isNotBlank();
        verify(repository).save(any(User.class));
    }

    @Test
    void handle_usernameExactMaxLength255_validationPasses() {
        // given — username at exact max boundary (255 chars) should pass validation
        final String longUsername = "a".repeat(255);
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username(longUsername)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername(longUsername)).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        final String token = sut.handle(userRegistrationCommand);

        // then
        assertThat(token).isNotBlank();
        verify(repository).save(any(User.class));
    }

    @Test
    void handle_usernameOneAboveMaxLength256_constraintViolationException() {
        // given — username exceeds @Size max (256 chars)
        final String tooLongUsername = "a".repeat(256);
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username(tooLongUsername)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_usernameOneAboveMaxLength_noPersistence() {
        // given — validation failure should prevent persistence
        final String tooLongUsername = "a".repeat(256);
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username(tooLongUsername)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        try {
            sut.handle(userRegistrationCommand);
        } catch (ConstraintViolationException ignored) {
        }

        // then
        verify(repository, never()).existsByUsername(any());
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_passwordExactMaxLength30_validationPasses() {
        // given — password at exact max boundary (30 chars) should pass validation
        final String maxPassword = "a".repeat(30);
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password(maxPassword)
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        final String token = sut.handle(userRegistrationCommand);

        // then
        assertThat(token).isNotBlank();
        verify(repository).save(any(User.class));
    }

    @Test
    void handle_passwordOneAboveMaxLength31_constraintViolationException() {
        // given — password exceeds max (31 chars)
        final String tooLongPassword = "a".repeat(31);
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password(tooLongPassword)
                .role(RoleDTO.ROLE_STUDENT)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(userRegistrationCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_eventContainsExactUsernameAndEmail() {
        // given — verify the integration event carries exact field values from the saved user
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("precise@example.com")
                .username("precise-user")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("precise-user")).thenReturn(false);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(userRegistrationCommand);

        // then
        final ArgumentCaptor<UserCreatedIntegrationEvent> eventCaptor =
                ArgumentCaptor.forClass(UserCreatedIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().username()).isEqualTo("precise-user");
        assertThat(eventCaptor.getValue().email()).isEqualTo("precise@example.com");
    }

    @Test
    void handle_usernameAlreadyExists_exceptionMessageContainsExactUsername() {
        // given
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("dup@gmail.com")
                .username("duplicate-name")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("duplicate-name")).thenReturn(true);

        // when / then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> sut.handle(userRegistrationCommand))
                .withMessageContaining("duplicate-name");
    }
}
