package com.educational.platform.users.jpa;

import com.educational.platform.common.exception.UnprocessableEntityException;
import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.login.SignInCommand;
import com.educational.platform.users.login.SignInCommandHandler;
import com.educational.platform.users.registration.UserRegistrationCommand;
import com.educational.platform.users.registration.UserRegistrationCommandHandler;
import com.educational.platform.users.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
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
 * Drives the real production command handlers ({@link UserRegistrationCommandHandler#handle} write path and
 * {@link SignInCommandHandler#handle} read path) against the <em>production schema configuration</em>: the
 * {@code custom_user} table is built by the real Liquibase changelog ({@code db/users.yml}, whose
 * {@code add-version-column-to-custom_user} changeSet adds {@code version BIGINT NOT NULL}) and Hibernate runs
 * with {@code ddl-auto=none}, exactly as in production.
 *
 * <p>This closes a seam the rest of the suite leaves open. The production-flow tests
 * ({@link com.educational.platform.users.registration.UserRegistrationVersioningTest},
 * {@link com.educational.platform.users.login.SignInVersioningTest},
 * {@link com.educational.platform.users.security.MyUserDetailsVersioningTest}) all run against the default
 * {@code @DataJpaTest} Hibernate-generated {@code INTEGER} schema, while
 * {@link UserOptimisticLockingOnMigratedSchemaTest} exercises the genuinely migrated {@code BIGINT} column but
 * only through raw {@code repository}/{@code EntityManager} operations - never the production handlers. So no
 * test proves the registration handler persists a {@code @Version}-bearing aggregate, nor that the sign-in
 * handler reads one back, when bound to the migration-owned {@code BIGINT} column under {@code ddl-auto=none} -
 * the only configuration that ships. A renamed/retyped {@code version} column or the entity drifting from the
 * migration would pass every production-flow test above (Hibernate schema) yet break the real production flow
 * here, on the schema that actually deploys.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class UserVersioningProductionFlowOnMigratedSchemaTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";

    @Autowired
    private UserRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final List<Object> publishedEvents = new ArrayList<>();

    private JwtTokenProvider jwtTokenProvider;
    private UserRegistrationCommandHandler registrationHandler;
    private SignInCommandHandler signInHandler;

    @BeforeEach
    void setUp() throws Exception {
        applyProductionMigration();

        jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn(TOKEN);
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final ApplicationEventPublisher eventPublisher = publishedEvents::add;
        registrationHandler = new UserRegistrationCommandHandler(
                new TransactionTemplate(transactionManager),
                passwordEncoder,
                jwtTokenProvider,
                repository,
                eventPublisher,
                validator);

        // a stub manager that accepts the credentials, so the sign-in flow reaches the role-resolution read path
        final AuthenticationManager authenticationManager = authentication -> authentication;
        signInHandler = new SignInCommandHandler(jwtTokenProvider, repository, validator, authenticationManager);
    }

    private void applyProductionMigration() throws Exception {
        // build the schema the way production does - via the Liquibase changelog, not Hibernate. With
        // ddl-auto=none Hibernate never touches the schema, so without this the table would not exist.
        // Re-running per test is idempotent: createTable is MARK_RAN (table already present) and the version
        // changeSet is recorded once, so subsequent tests reuse the same migrated schema.
        try (Connection connection = dataSource.getConnection()) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase("db/users.yml", new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    @Test
    void registrationHandler_onMigratedSchema_persistsAggregateAtVersionZero() {
        // when the real registration write path runs against the migrated BIGINT column (validate -> save inside
        // the handler's transaction)
        registrationHandler.handle(command(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));

        // force a genuine DB round trip so the version is read back from the row, not the in-context instance
        entityManager.flush();
        entityManager.clear();

        // then the production write path persists a @Version-bearing aggregate onto the migration-owned column:
        // the Integer @Version maps cleanly onto BIGINT and JPA initialises it to 0, while the read projection is
        // unaffected. UserRegistrationVersioningTest pins this on the Hibernate-generated INTEGER schema only;
        // this is its missing production-schema counterpart (the only configuration that ships).
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
        final UserDTO dto = reloaded.toDTO();
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void registrationHandler_duplicateUsername_onMigratedSchema_isRejected() {
        // given a user already registered through the production write path on the migrated schema
        registrationHandler.handle(command(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));

        // when the same username is registered again
        // then the existsByUsername guard still rejects it against the migration-owned table: adding @Version does
        // not disturb the registration flow's duplicate-username contract on the schema that actually deploys
        assertThatExceptionOfType(UnprocessableEntityException.class)
                .isThrownBy(() -> registrationHandler.handle(command(USERNAME, "other@gmail.com", RoleDTO.ROLE_STUDENT)));
    }

    @Test
    void signInHandler_onMigratedSchema_resolvesRoleFromVersionedUser() {
        // given a teacher persisted onto the migrated BIGINT column (the non-default role, to pin the role maps
        // through the read path unchanged)
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        entityManager.clear();

        // when the real sign-in read path runs (authenticate -> load the persisted aggregate -> resolve its role)
        final String token = signInHandler.handle(signInCommand("teacher"));

        // then the production read path is unaffected by @Version on the migration-owned column: a token is issued
        // and the role resolved from the version-bearing user is the persisted role - never the optimistic-lock
        // version. SignInVersioningTest pins this on the Hibernate-generated INTEGER schema only; this is its
        // missing production-schema counterpart (the only configuration that ships).
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

    private Object versionOf(final String username) {
        return entityManager
                .createNativeQuery("SELECT version FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult();
    }

    private SignInCommand signInCommand(final String username) {
        return SignInCommand.builder()
                .username(username)
                .password(PASSWORD)
                .build();
    }

    private User newUser(final String username, final String email, final RoleDTO role) {
        return new User(command(username, email, role), passwordEncoder);
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
