package com.educational.platform.users.login;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.security.JwtTokenProvider;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class SignInCommandHandlerTest {

    @Mock
    private UserRepository repository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SignInCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = spy(new SignInCommandHandler(jwtTokenProvider, repository, validator, authenticationManager));
    }

    @Test
    void handle_validCommand_signedIn() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        final String token = sut.handle(signInCommand);

        // then
        assertThat(token)
                .isEqualTo("token");
    }

    @Test
    void handle_invalidUsernamePassword_unprocessableEntityException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        doThrow(BadCredentialsException.class).when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class).isThrownBy(handle);
    }

    @Test
    void handle_usernameIsEmpty_constraintViolationException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .password("password")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_passwordIsEmpty_constraintViolationException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_authenticationManagerCalledWithCorrectCredentials() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("myuser")
                .password("mypassword")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("myuser")
                .password("mypassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("myuser")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then
        verify(authenticationManager).authenticate(argThat(auth ->
                auth.getPrincipal().equals("myuser") && auth.getCredentials().equals("mypassword")
        ));
    }

    @Test
    void handle_validCommand_createTokenCalledWithCorrectUsernameAndRole() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then
        verify(jwtTokenProvider).createToken(eq("username"), eq(Collections.singletonList(Role.ROLE_STUDENT)));
    }

    @Test
    void handle_teacherUser_createTokenCalledWithTeacherRole() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("teacher")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("teacher@gmail.com")
                .username("teacher")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("teacher")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("teacher-token");

        // when
        final String token = sut.handle(signInCommand);

        // then
        assertThat(token).isEqualTo("teacher-token");
        verify(jwtTokenProvider).createToken(eq("teacher"), eq(Collections.singletonList(Role.ROLE_TEACHER)));
    }

    @Test
    void handle_validCommand_repositoryQueriedWithCorrectUsername() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("testuser")
                .password("testpass")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("test@gmail.com")
                .username("testuser")
                .password("testpass")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then
        verify(repository).findByUsername("testuser");
    }

    @Test
    void handle_invalidCredentials_exceptionMessageContainsDetail() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("wrong")
                .build();
        doThrow(BadCredentialsException.class).when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_userNotFoundAfterAuthentication_noSuchElementException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("ghost")
                .password("password")
                .build();
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(java.util.NoSuchElementException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_returnsToken() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("expected-token");

        // when
        final String token = sut.handle(signInCommand);

        // then
        assertThat(token).isEqualTo("expected-token");
    }

    @Test
    void handle_invalidCredentials_repositoryNeverQueried() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("wrong")
                .build();
        doThrow(BadCredentialsException.class).when(authenticationManager).authenticate(any());

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).findByUsername(any());
    }

    @Test
    void handle_invalidCredentials_tokenNeverCreated() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("wrong")
                .build();
        doThrow(BadCredentialsException.class).when(authenticationManager).authenticate(any());

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_validationFails_authenticationManagerNeverCalled() {
        // given — null username triggers @NotBlank violation
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password("password")
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void handle_validationFails_repositoryNeverQueried() {
        // given — null username triggers @NotBlank violation
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password("password")
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).findByUsername(any());
    }

    @Test
    void handle_validationFails_tokenNeverCreated() {
        // given — null password triggers @NotBlank violation
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password(null)
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_validCommand_authenticatesBeforeRepositoryQuery() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("orderuser")
                .password("orderpass")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("order@gmail.com")
                .username("orderuser")
                .password("orderpass")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("orderuser")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then — authenticate must be called before findByUsername
        final var inOrder = inOrder(authenticationManager, repository);
        inOrder.verify(authenticationManager).authenticate(any());
        inOrder.verify(repository).findByUsername("orderuser");
    }

    @Test
    void handle_authenticationExceptionSubclass_wrappedInUnprocessableEntityException() {
        // given — AccountExpiredException is an AuthenticationException (not BadCredentialsException)
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("expired")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.AccountExpiredException("Account expired"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then — any AuthenticationException subclass should be caught
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_bothFieldsNull_constraintViolationException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password(null)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_blankUsername_constraintViolationException() {
        // given — whitespace-only username triggers @NotBlank
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("   ")
                .password("password")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_blankPassword_constraintViolationException() {
        // given — whitespace-only password triggers @NotBlank
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("   ")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_bothFieldsNull_authenticationManagerNeverCalled() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password(null)
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void handle_blankUsername_repositoryNeverQueried() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("   ")
                .password("password")
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(repository, never()).findByUsername(any());
    }

    @Test
    void handle_emptyUsername_constraintViolationException() {
        // given — empty string triggers @NotBlank
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("")
                .password("password")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_emptyPassword_constraintViolationException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validCommand_tokenIsNotBlank() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("a-valid-token");

        // when
        final String token = sut.handle(signInCommand);

        // then
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void handle_bothFieldsBlank_constraintViolationException() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("   ")
                .password("   ")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_validationFails_noSideEffects() {
        // given — both null triggers validation failure
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password(null)
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then
        verify(authenticationManager, never()).authenticate(any());
        verify(repository, never()).findByUsername(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_validCommand_ordering_authenticateThenQueryThenCreateToken() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then — verify ordering: authenticate → findByUsername → createToken
        final var inOrderVerifier = inOrder(authenticationManager, repository, jwtTokenProvider);
        inOrderVerifier.verify(authenticationManager).authenticate(any());
        inOrderVerifier.verify(repository).findByUsername("username");
        inOrderVerifier.verify(jwtTokenProvider).createToken(eq("username"), eq(Collections.singletonList(Role.ROLE_STUDENT)));
    }

    @Test
    void handle_emptyStringUsername_noSideEffects() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("")
                .password("password")
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then — validation short-circuits all downstream interactions
        verify(authenticationManager, never()).authenticate(any());
        verify(repository, never()).findByUsername(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_emptyStringPassword_noSideEffects() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("")
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (Exception ignored) {
        }

        // then — validation short-circuits all downstream interactions
        verify(authenticationManager, never()).authenticate(any());
        verify(repository, never()).findByUsername(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }

    @Test
    void handle_disabledException_wrappedInUnprocessableEntityException() {
        // given — DisabledException is an AuthenticationException subclass
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("disabled")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.DisabledException("User is disabled"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_lockedException_wrappedInUnprocessableEntityException() {
        // given — LockedException is an AuthenticationException subclass
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("locked")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.LockedException("User account is locked"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_constraintViolationException_containsExpectedPropertyPaths() {
        // given — null username triggers @NotBlank violation with property path
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password("password")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(signInCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations())
                        .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void handle_nullPassword_constraintViolationContainsPasswordPath() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(signInCommand))
                .satisfies(ex -> assertThat(ex.getConstraintViolations())
                        .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void handle_bothFieldsNull_constraintViolationContainsBothPaths() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username(null)
                .password(null)
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.handle(signInCommand))
                .satisfies(ex -> {
                    assertThat(ex.getConstraintViolations())
                            .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
                    assertThat(ex.getConstraintViolations())
                            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
                });
    }

    @Test
    void handle_validCommand_authenticationManagerCalledExactlyOnce() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void handle_validCommand_repositoryQueriedExactlyOnce() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then
        verify(repository, times(1)).findByUsername("username");
    }

    @Test
    void handle_validCommand_tokenCreatedExactlyOnce() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("token");

        // when
        sut.handle(signInCommand);

        // then
        verify(jwtTokenProvider, times(1)).createToken(any(), any());
    }

    @Test
    void handle_credentialsExpiredException_wrappedInUnprocessableEntityException() {
        // given — CredentialsExpiredException is an AuthenticationException subclass
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("expiredcreds")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.CredentialsExpiredException("User credentials have expired"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_authenticationServiceException_wrappedInUnprocessableEntityException() {
        // given — AuthenticationServiceException is an AuthenticationException subclass (server-side auth failure)
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.AuthenticationServiceException("Authentication service unavailable"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_providerNotFoundException_wrappedInUnprocessableEntityException() {
        // given — ProviderNotFoundException is an AuthenticationException subclass
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.ProviderNotFoundException("No AuthenticationProvider found"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_internalAuthenticationServiceException_wrappedInUnprocessableEntityException() {
        // given — InternalAuthenticationServiceException (server error during auth) is still wrapped
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        doThrow(new org.springframework.security.authentication.InternalAuthenticationServiceException("Internal error"))
                .when(authenticationManager).authenticate(any());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(handle)
                .withMessageContaining("Invalid username/password");
    }

    @Test
    void handle_validCommand_returnedTokenIsFromProvider() {
        // given
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("password")
                .build();
        final UserRegistrationCommand userRegistrationCommand = UserRegistrationCommand.builder()
                .email("email@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        final User existingUser = new User(userRegistrationCommand, passwordEncoder);
        when(repository.findByUsername("username")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("specific-generated-token");

        // when
        final String result = sut.handle(signInCommand);

        // then — the token returned must be exactly what the provider produced
        assertThat(result).isEqualTo("specific-generated-token");
    }

    @Test
    void handle_whitespaceOnlyUsername_constraintViolationException() {
        // given — whitespace-only username should be caught by @NotBlank
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("   ")
                .password("password")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_whitespaceOnlyPassword_constraintViolationException() {
        // given — whitespace-only password should be caught by @NotBlank
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("username")
                .password("   ")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(signInCommand);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(handle);
    }

    @Test
    void handle_whitespaceOnlyUsername_noAuthenticationAttempted() {
        // given — validation failure should prevent authentication
        final SignInCommand signInCommand = SignInCommand.builder()
                .username("   ")
                .password("password")
                .build();

        // when
        try {
            sut.handle(signInCommand);
        } catch (ConstraintViolationException ignored) {
        }

        // then
        verify(authenticationManager, never()).authenticate(any());
        verify(repository, never()).findByUsername(any());
        verify(jwtTokenProvider, never()).createToken(any(), any());
    }
}
