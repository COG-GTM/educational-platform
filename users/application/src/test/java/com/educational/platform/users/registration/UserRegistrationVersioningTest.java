package com.educational.platform.users.registration;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration coverage that drives the real {@link UserRegistrationCommandHandler#handle} write path against
 * a database, exercising the {@code @Version} optimistic-locking field added to {@link User} through the actual
 * production flow rather than a synthetic test-only write.
 *
 * <p>The {@code jpa} tests prove version semantics via {@code saveAndFlush} / forced increments, and the existing
 * {@link UserRegistrationCommandHandlerTest} drives the handler against a mocked repository (so JPA never assigns
 * a version). This pins the remaining gap: registering a user via {@code repository.save(...)} inside the handler's
 * {@code TransactionTemplate} persists an aggregate whose {@code @Version} is initialised by JPA, and that adding
 * {@code @Version} leaves the handler's existing duplicate-username and integration-event contracts intact.
 */
@DataJpaTest
class UserRegistrationVersioningTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";

    @Autowired
    private UserRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<Object> publishedEvents = new ArrayList<>();

    private JwtTokenProvider jwtTokenProvider;
    private UserRegistrationCommandHandler handler;

    @BeforeEach
    void setUp() {
        final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn(TOKEN);
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final ApplicationEventPublisher eventPublisher = publishedEvents::add;
        handler = new UserRegistrationCommandHandler(
                new TransactionTemplate(transactionManager),
                passwordEncoder,
                jwtTokenProvider,
                repository,
                eventPublisher,
                validator);
    }

    @Test
    void handle_validCommand_persistsUserWithInitialVersionZero() {
        // when the real registration flow runs (validate -> save inside the handler's transaction)
        handler.handle(command(USERNAME, EMAIL));

        // force a genuine DB round trip so the version is read back from the row, not the in-context instance
        entityManager.flush();
        entityManager.clear();

        // then the aggregate persisted through the production write path - not just a synthetic saveAndFlush -
        // carries the JPA-initialised optimistic-lock version (0), and its read projection is unaffected
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
        final UserDTO dto = reloaded.toDTO();
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void handle_validCommand_emitsIntegrationEventUnaffectedByVersion() {
        // when the real flow registers a user (which now persists a @Version-bearing aggregate)
        handler.handle(command(USERNAME, EMAIL));

        // then the single cross-context event is still emitted with exactly username/email: adding @Version to the
        // aggregate must not disturb - nor leak the optimistic-lock version onto - the published integration event
        assertThat(publishedEvents).singleElement()
                .isInstanceOfSatisfying(UserCreatedIntegrationEvent.class, event -> {
                    assertThat(event).hasFieldOrPropertyWithValue("username", USERNAME);
                    assertThat(event).hasFieldOrPropertyWithValue("email", EMAIL);
                });
    }

    @Test
    void handle_duplicateUsername_unprocessableEntityException_withVersionColumnPresent() {
        // given an already-registered user (persisted with the new @Version column in place)
        handler.handle(command(USERNAME, EMAIL));

        // when the same username is registered again
        // then the existsByUsername guard still rejects it: introducing @Version does not disturb the
        // existing duplicate-username contract of the registration flow
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> handler.handle(command(USERNAME, "other@gmail.com")));
    }

    @Test
    void handle_validTeacherCommand_persistsUserWithInitialVersionZero() {
        // when the real registration flow persists a teacher (the non-default role) through the production write path
        handler.handle(command("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));

        // force a genuine DB round trip so the version is read back from the row, not the in-context instance
        entityManager.flush();
        entityManager.clear();

        // then the teacher aggregate persisted through the production write path carries the JPA-initialised
        // optimistic-lock version (0) and its non-default role projection is unaffected by @Version - completes the
        // role coverage of the registration write path (handle_validCommand_... only covers the student role)
        final User reloaded = repository.findByUsername("teacher").orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
        assertThat(reloaded.toDTO().role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void handle_distinctUsers_eachPersistedAtIndependentVersionZero() {
        // when two distinct users are registered through the real production write path
        handler.handle(command(USERNAME, EMAIL));
        handler.handle(command("other", "other@gmail.com"));

        // force a genuine DB round trip so each version is read back from its row
        entityManager.flush();
        entityManager.clear();

        // then version initialisation is per-row through the production handler: registering a second user neither
        // shares nor advances the first user's optimistic-lock version - both fresh inserts start at 0
        // (handle_validCommand_persistsUserWithInitialVersionZero only exercises a single registration)
        assertThat(repository.findByUsername(USERNAME).orElseThrow()).hasFieldOrPropertyWithValue("version", 0);
        assertThat(repository.findByUsername("other").orElseThrow()).hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    void handle_whenAnotherUsersVersionHasAdvanced_persistsNewUserAtIndependentVersionZero() {
        // given a user registered through the real handler whose optimistic-lock version has since advanced to 1
        handler.handle(command(USERNAME, EMAIL));
        entityManager.flush();
        forceIncrementVersionOf(USERNAME);

        // when a brand-new user is subsequently registered through the same production write path
        handler.handle(command("other", "other@gmail.com"));

        // force a genuine DB round trip so each version is read back from its row
        entityManager.flush();
        entityManager.clear();

        // then version *initialisation* is per-row through the handler: the new row starts at 0 regardless of the
        // advanced version on the pre-existing row, while that row keeps its advanced version. This is the
        // production-handler counterpart to UserOptimisticLockingTest.save_newUser_whenAnotherUsersVersionHasAdvanced_startsAtZero,
        // and is distinct from handle_distinctUsers_eachPersistedAtIndependentVersionZero, which only proves two
        // *fresh* inserts both start at 0 (write isolation) without any pre-existing row having advanced first.
        assertThat(repository.findByUsername("other").orElseThrow()).hasFieldOrPropertyWithValue("version", 0);
        assertThat(repository.findByUsername(USERNAME).orElseThrow()).hasFieldOrPropertyWithValue("version", 1);
    }

    @Test
    void handle_resolvesStudentRoleIntoIssuedToken_unaffectedByVersion() {
        // when the real registration flow runs and returns the token issued for the freshly persisted aggregate
        final String token = handler.handle(command(USERNAME, EMAIL));

        // then the handler's return-value contract holds under @Version: it issues the token built from the
        // just-persisted, JPA-version-initialised aggregate, and the role carried into it is the student role
        // resolved via toDTO().role() - never the optimistic-lock version. The other registration tests assert the
        // persisted row's projection and the emitted event but never the issued token; SignInVersioningTest pins
        // this role-into-token resolution for the *read* path (handle_persistedVersionedUser_signsInAndResolvesStudentRole,
        // via the same ArgumentCaptor) - this is its missing write-path counterpart.
        assertThat(token).isEqualTo(TOKEN);
        assertThat(rolesIssuedInTokenFor(USERNAME)).containsExactly(Role.ROLE_STUDENT);
    }

    @Test
    void handle_resolvesTeacherRoleIntoIssuedToken_unaffectedByVersion() {
        // when a teacher (the non-default role) is registered through the real production write path
        final String token = handler.handle(command("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));

        // then the non-default role is resolved into the issued token from the @Version-bearing aggregate too,
        // completing the role coverage of the token-issuance path (handle_resolvesStudentRoleIntoIssuedToken_...
        // only covers the student role) - the write-path counterpart to
        // SignInVersioningTest.handle_teacher_resolvesTeacherRoleThroughSignInReadPath
        assertThat(token).isEqualTo(TOKEN);
        assertThat(rolesIssuedInTokenFor("teacher")).containsExactly(Role.ROLE_TEACHER);
    }

    @SuppressWarnings("unchecked")
    private List<Role> rolesIssuedInTokenFor(final String username) {
        // the role passed to createToken is resolved from the just-persisted aggregate via toDTO().role(), so
        // capturing it pins which role the @Version-bearing entity carried into the issued token
        final ArgumentCaptor<List<Role>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(jwtTokenProvider).createToken(eq(username), rolesCaptor.capture());
        return rolesCaptor.getValue();
    }

    private void forceIncrementVersionOf(final String username) {
        entityManager.clear();
        final User loaded = repository.findByUsername(username).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
    }

    private UserRegistrationCommand command(final String username, final String email) {
        return command(username, email, RoleDTO.ROLE_STUDENT);
    }

    private UserRegistrationCommand command(final String username, final String email, final RoleDTO role) {
        return UserRegistrationCommand.builder()
                .username(username)
                .email(email)
                .password(PASSWORD)
                .role(role)
                .build();
    }
}
