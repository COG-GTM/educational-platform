package com.educational.platform.users.registration;

import com.educational.platform.users.Role;
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

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationCommandHandlerSuccessTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

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
    void handle_validCommand_passwordEncoded() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("user@gmail.com")
                .username("username")
                .password("rawpassword")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encodedpassword");
        when(jwtTokenProvider.createToken(eq("username"), any())).thenReturn("jwt-token");

        // when
        sut.handle(command);

        // then
        verify(passwordEncoder).encode("rawpassword");
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        final User user = argument.getValue();
        assertThat(user)
                .hasFieldOrPropertyWithValue("password", "encodedpassword");
    }

    @Test
    void handle_validCommand_jwtTokenReturned() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("user@gmail.com")
                .username("username")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("username")).thenReturn(false);
        when(jwtTokenProvider.createToken(eq("username"), eq(Collections.singletonList(Role.ROLE_STUDENT)))).thenReturn("jwt-token");

        // when
        final String result = sut.handle(command);

        // then
        assertThat(result).isEqualTo("jwt-token");
    }

    @Test
    void handle_validStudentCommand_userSavedWithStudentRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("student@gmail.com")
                .username("student")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("student")).thenReturn(false);
        when(jwtTokenProvider.createToken(eq("student"), any())).thenReturn("token");

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(repository).save(argument.capture());
        final User user = argument.getValue();
        assertThat(user)
                .hasFieldOrPropertyWithValue("username", "student")
                .hasFieldOrPropertyWithValue("email", "student@gmail.com")
                .hasFieldOrPropertyWithValue("role", Role.ROLE_STUDENT);
    }
}
