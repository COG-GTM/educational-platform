package com.educational.platform.users.jpa;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserDTO;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage that the {@code @Version} field added to {@link User} does not disturb the entity's
 * existing persistence/finder contract - the by-id read path, the derived existence query and identity
 * generation - running against the <em>production schema configuration</em>: the {@code custom_user} table is
 * built by the real Liquibase changelog ({@code db/users.yml}, whose {@code add-version-column-to-custom_user}
 * changeSet adds {@code version BIGINT NOT NULL}) and Hibernate runs with {@code ddl-auto=none}, exactly as in
 * production.
 *
 * <p>This closes the one seam the rest of the suite leaves open. {@link UserPersistenceTest} pins this
 * persistence contract - {@code findById} preserving both projections at version 0 and after an increment,
 * {@code existsByUsername} reflecting present/absent users, and a generated positive primary key coexisting
 * with the version field - but only against the default {@code @DataJpaTest} Hibernate-generated {@code INTEGER}
 * schema. {@link UserVersioningProductionFlowOnMigratedSchemaTest} drives the production handlers against the
 * migrated {@code BIGINT} column, yet those flows only ever read by username ({@code findByUsername}) - never
 * the {@code JpaRepository} primary-key finder, never the {@code existsByUsername} present/absent contract in
 * isolation (it surfaces only indirectly through the duplicate-username rejection), and never the
 * id-generation guarantee. So the entity's core persistence contract is unverified on the schema that actually
 * ships: a renamed/retyped {@code version} column, or @Version drifting from the migration, could leave the
 * by-id read path or identity generation broken under {@code ddl-auto=none} while every {@link UserPersistenceTest}
 * case (Hibernate schema) still passes. This is the migrated-schema counterpart of {@link UserPersistenceTest},
 * mirroring the Hibernate-schema -> migrated-schema pairing the rest of the suite establishes (e.g.
 * {@link UserOptimisticLockingTest} -> {@link UserOptimisticLockingOnMigratedSchemaTest}).
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class UserPersistenceOnMigratedSchemaTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String RAW_PASSWORD = "password";

    @Autowired
    private UserRepository repository;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void applyProductionMigration() throws Exception {
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
    void findById_afterPersist_preservesProjectionsAndAssignsVersionZero() {
        // given a user persisted onto the migration-owned BIGINT column
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        final int id = idOf(USERNAME);
        entityManager.clear();

        // when it is read back through the primary-key finder - the read path the handler flows never use
        // (they read by username), and the only finder not exercised on the schema that ships
        final User reloaded = repository.findById(id).orElseThrow();

        // then loading by id is unaffected by the new @Version/@Id pairing on the BIGINT column: both projections
        // round-trip and a freshly persisted row reports version 0. UserPersistenceTest pins this on the
        // Hibernate-generated INTEGER schema only; this is its missing production-schema counterpart.
        final UserDTO dto = reloaded.toDTO();
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(reloaded.toUserDetails().getUsername()).isEqualTo(USERNAME);
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    void findById_afterVersionIncrement_preservesProjectionsAtNonZeroVersion() {
        // given a persisted user whose version has since been bumped to 1 on the migrated schema
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        final int id = idOf(USERNAME);
        forceIncrementVersionOf(USERNAME);
        entityManager.clear();

        // when the entity is read back through the primary-key finder at its incremented version
        final User reloaded = repository.findById(id).orElseThrow();

        // then the by-id read path keeps projecting correctly once the version is non-zero on the BIGINT column:
        // an advanced version never leaks into the by-id projections. findById is otherwise only pinned at
        // version 0 here, while the after-increment cell is covered on the Hibernate-generated INTEGER schema
        // (UserPersistenceTest.findById_afterVersionIncrement_preservesProjectionsAtNonZeroVersion) - not on the
        // schema that ships.
        final UserDTO dto = reloaded.toDTO();
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);

        final UserDetails userDetails = reloaded.toUserDetails();
        assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_STUDENT.getAuthority());
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 1);
    }

    @Test
    void findById_teacher_afterPersist_preservesTeacherRoleProjection() {
        // given a teacher (the non-default role) persisted onto the migrated BIGINT column
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        final int id = idOf("teacher");
        entityManager.clear();

        // when it is read back through the primary-key finder
        final User reloaded = repository.findById(id).orElseThrow();

        // then the non-default role maps through both by-id projections unchanged by @Version on the BIGINT
        // column, and a freshly persisted row reports version 0 - completing the role x by-id-read matrix on the
        // production schema (the student-role by-id cases above only cover ROLE_STUDENT). UserPersistenceTest
        // pins the teacher-by-id cell on the Hibernate-generated INTEGER schema only.
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
        assertThat(reloaded.toDTO().role()).isEqualTo(RoleDTO.ROLE_TEACHER);
        assertThat(reloaded.toUserDetails().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_TEACHER.getAuthority());
    }

    @Test
    void existsByUsername_reflectsPersistedAndAbsentUsers() {
        // given a single persisted user on the migrated schema
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));

        // then the derived existence query reports presence for the persisted username and absence for an unknown
        // one against the migration-owned table: adding @Version does not disturb existsByUsername on the schema
        // that ships. The production-flow test only exercises this query indirectly (through the duplicate-username
        // rejection); UserPersistenceTest pins the present/absent contract directly but on the Hibernate INTEGER
        // schema only.
        assertThat(repository.existsByUsername(USERNAME)).isTrue();
        assertThat(repository.existsByUsername("missing")).isFalse();
    }

    @Test
    void existsByUsername_afterVersionIncrement_stillReportsTrue() {
        // given a persisted user whose version has since been bumped to 1 on the migrated schema
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        forceIncrementVersionOf(USERNAME);
        entityManager.clear();

        // then the derived existence query keeps working once the version is non-zero on the BIGINT column
        // (existsByUsername_reflectsPersistedAndAbsentUsers only exercises version 0); UserPersistenceTest pins
        // this after-increment cell on the Hibernate-generated INTEGER schema only.
        assertThat(repository.existsByUsername(USERNAME)).isTrue();
    }

    @Test
    void persist_assignsGeneratedIdAlongsideVersion() {
        // given / when a transient user is persisted onto the migrated schema
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));

        // then introducing @Version next to @Id leaves identity generation intact on the migration-owned table:
        // the row still receives a generated, positive primary key while the BIGINT version initialises to 0.
        // UserPersistenceTest pins id-generation on the Hibernate-generated INTEGER schema only - this is its
        // production-schema counterpart, where @Id and the migration's version column must coexist.
        assertThat(idOf(USERNAME)).isPositive();
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    private int idOf(final String username) {
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult()).intValue();
    }

    private Object versionOf(final String username) {
        return entityManager
                .createNativeQuery("SELECT version FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult();
    }

    private void forceIncrementVersionOf(final String username) {
        entityManager.clear();
        final User loaded = repository.findByUsername(username).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
    }

    private User newUser(final String username, final String email, final RoleDTO role) {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username(username)
                .email(email)
                .password(RAW_PASSWORD)
                .role(role)
                .build();
        return new User(command, passwordEncoder);
    }
}
