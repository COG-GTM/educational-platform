package com.educational.platform.users.login;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration coverage driving the real {@link SignInCommandHandler#handle} read path against a database, so the
 * {@code @Version} optimistic-locking field added to {@link User} is exercised through the actual sign-in flow.
 *
 * <p>{@link com.educational.platform.users.registration.UserRegistrationVersioningTest} pins the write path
 * (registration persists a {@code @Version}-bearing aggregate) and
 * {@link com.educational.platform.users.security.MyUserDetailsVersioningTest} pins the {@code UserDetailsService}
 * read path. This pins the remaining production read path: after authentication, {@code SignInCommandHandler}
 * resolves the caller's role via {@code repository.findByUsername(...).get().toDTO().role()} - a genuine DB
 * round trip of a persisted {@code @Version}-bearing user. The existing {@link SignInCommandHandlerTest} only
 * drives the handler against a <strong>mocked</strong> repository returning an in-memory, never-persisted user
 * (its {@code @Version} is {@code null}), so this column->field read through the sign-in flow is otherwise untested.
 */
@DataJpaTest
class SignInVersioningTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";

    @Autowired
    private UserRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private JwtTokenProvider jwtTokenProvider;
    private SignInCommandHandler handler;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn(TOKEN);
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        // a stub manager that accepts the credentials, so the flow reaches the role-resolution read path
        final AuthenticationManager authenticationManager = authentication -> authentication;
        handler = new SignInCommandHandler(jwtTokenProvider, repository, validator, authenticationManager);
    }

    @Test
    void handle_persistedVersionedUser_signsInAndResolvesStudentRole() {
        // given a student persisted with the new @Version column in place
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        entityManager.clear();

        // when the real sign-in flow runs (authenticate -> load the persisted aggregate -> resolve its role)
        final String token = handler.handle(command(USERNAME));

        // then the sign-in read path is unaffected by @Version: a token is issued and the role resolved from the
        // persisted, version-bearing user is the student role - never the optimistic-lock version
        assertThat(token).isEqualTo(TOKEN);
        assertThat(rolesResolvedFor(USERNAME)).containsExactly(Role.ROLE_STUDENT);
    }

    @Test
    void handle_afterVersionIncrement_stillSignsInAndResolvesRole() {
        // given a persisted user whose version has since been bumped to 1
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // when the user signs in at a non-zero version
        final String token = handler.handle(command(USERNAME));

        // then a non-zero version never disturbs the sign-in read path
        // (handle_persistedVersionedUser_... only exercises version 0)
        assertThat(token).isEqualTo(TOKEN);
        assertThat(rolesResolvedFor(USERNAME)).containsExactly(Role.ROLE_STUDENT);
    }

    @Test
    void handle_teacher_resolvesTeacherRoleThroughSignInReadPath() {
        // given a teacher persisted with the new @Version column in place
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        entityManager.clear();

        // when the teacher signs in
        final String token = handler.handle(command("teacher"));

        // then the non-default role maps through the sign-in read path unchanged by @Version
        assertThat(token).isEqualTo(TOKEN);
        assertThat(rolesResolvedFor("teacher")).containsExactly(Role.ROLE_TEACHER);
    }

    @SuppressWarnings("unchecked")
    private List<Role> rolesResolvedFor(final String username) {
        // the role passed to the token is resolved from the persisted user the handler reads back by username,
        // so capturing it pins what the @Version-bearing entity yielded through the sign-in read path
        final ArgumentCaptor<List<Role>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(jwtTokenProvider).createToken(eq(username), rolesCaptor.capture());
        return rolesCaptor.getValue();
    }

    private SignInCommand command(final String username) {
        return SignInCommand.builder()
                .username(username)
                .password(PASSWORD)
                .build();
    }

    private User newUser(final String username, final String email, final RoleDTO role) {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username(username)
                .email(email)
                .password(PASSWORD)
                .role(role)
                .build();
        return new User(command, passwordEncoder);
    }
}
