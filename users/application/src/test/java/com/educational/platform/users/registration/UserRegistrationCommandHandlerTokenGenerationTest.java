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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link UserRegistrationCommandHandler} generates a JWT token
 * using the correct username and role after registration.
 */
@ExtendWith(MockitoExtension.class)
public class UserRegistrationCommandHandlerTokenGenerationTest {

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
        sut = new UserRegistrationCommandHandler(transactionTemplate, passwordEncoder, jwtTokenProvider,
                repository, eventPublisher, validator);
    }

    @Test
    void handle_teacherRegistration_generatesTokenWithTeacherRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("teacher@school.com")
                .username("teacher1")
                .password("password")
                .role(RoleDTO.ROLE_TEACHER)
                .build();
        when(repository.existsByUsername("teacher1")).thenReturn(false);
        when(jwtTokenProvider.createToken("teacher1", Collections.singletonList(Role.ROLE_TEACHER)))
                .thenReturn("teacher-jwt-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("teacher-jwt-token");
        verify(jwtTokenProvider).createToken(eq("teacher1"), eq(Collections.singletonList(Role.ROLE_TEACHER)));
    }

    @Test
    void handle_studentRegistration_generatesTokenWithStudentRole() {
        // given
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .email("student@school.com")
                .username("student1")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        when(repository.existsByUsername("student1")).thenReturn(false);
        when(jwtTokenProvider.createToken("student1", Collections.singletonList(Role.ROLE_STUDENT)))
                .thenReturn("student-jwt-token");

        // when
        final String token = sut.handle(command);

        // then
        assertThat(token).isEqualTo("student-jwt-token");
        verify(repository).save(org.mockito.ArgumentMatchers.any(User.class));
    }
}
