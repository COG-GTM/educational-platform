package com.educational.platform.users.jpa;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * End-to-end coverage of the {@code @Version} optimistic locking on {@link User} running against the
 * <em>production schema configuration</em>: the {@code custom_user} table is created by the real Liquibase
 * changelog ({@code db/users.yml}, whose {@code add-version-column-to-custom_user} changeSet adds
 * {@code version BIGINT NOT NULL}) and Hibernate runs with {@code ddl-auto=none}, so the entity is mapped onto
 * the migration-owned schema exactly as in production.
 *
 * <p>This closes the one seam the rest of the suite leaves open:
 * <ul>
 *   <li>{@link UserOptimisticLockingTest} proves the locking <em>behaviour</em>, but against a
 *   <strong>Hibernate-generated</strong> schema (the default {@code @DataJpaTest} {@code ddl-auto=create-drop}),
 *   where the version column is an {@code INTEGER} derived from the entity field - never the migration's
 *   {@code BIGINT} column.</li>
 *   <li>{@link com.educational.platform.users.db.UserVersionColumnMigrationTest} proves the migration produces a
 *   {@code BIGINT NOT NULL} column, but with <strong>no entity</strong> in play (raw JDBC).</li>
 *   <li>{@link com.educational.platform.users.UserVersionMappingTest} pins the {@code @Version -> "version"}
 *   column mapping by <strong>reflection</strong>, never at runtime.</li>
 * </ul>
 * None of them prove the Integer-typed {@code @Version} field actually initialises, increments and detects
 * stale writes when bound to the genuinely migrated {@code BIGINT} column under {@code ddl-auto=none} - which is
 * the only configuration that ships. A renamed/retyped column, or the entity drifting from the migration, would
 * pass every test above yet break here, where optimistic locking is exercised on the real production schema.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class UserOptimisticLockingOnMigratedSchemaTest {

    private static final String USERNAME = "username";

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
    void schemaUnderTest_isTheMigratedBigIntColumn_notAHibernateGeneratedOne() throws SQLException {
        // precondition that gives every other test here its meaning: the JPA entity is running against the
        // migration-owned BIGINT column (ddl-auto=none + Liquibase), not the INTEGER column Hibernate would
        // derive from the Integer field had it generated the schema. This is the runtime counterpart to the
        // entity-less migration test and the reflection-only column-mapping test.
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column exists on the migrated custom_user").isTrue();
            assertThat(columns.getInt("DATA_TYPE")).as("version is the migration's BIGINT, not a generated INTEGER")
                    .isEqualTo(Types.BIGINT);
            assertThat(columns.getString("IS_NULLABLE")).isEqualTo("NO");
        }
    }

    @Test
    void persist_onMigratedSchema_initializesVersionToZero() {
        // when a registered aggregate is persisted onto the migrated BIGINT column
        final User saved = repository.saveAndFlush(newUser());

        // then the Integer @Version maps cleanly onto the BIGINT column and JPA initialises it to 0 - the
        // entity<->migration agreement holds at runtime, not just by reflection
        assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    @Test
    void write_onMigratedSchema_incrementsVersionAndReadsBackAsInteger() {
        // given a user persisted on the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when the row is written (forced increment exercises the @Version column)
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the version advances on the BIGINT column and materialises back onto the Integer-typed field -
        // proving the intentional Integer<->BIGINT decoupling round-trips on the production schema
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 1);
        assertThat(loaded).extracting("version").isInstanceOf(Integer.class);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void staleWrite_onMigratedSchema_throwsOptimisticLockException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the raw EntityManager path
        // then optimistic locking fires on the BIGINT column just as it does on the Hibernate-generated one
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void staleMerge_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (the state a request holds between read and write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path against the migrated schema
        // then the conflict surfaces as Spring's translated exception, not a silent overwrite - the other
        // production write path (repository.save) is guarded on the real BIGINT column too
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void staleDelete_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted on the migrated schema
        repository.delete(stale);

        // then the delete path is guarded on the real BIGINT column too: the version check finds no row at the
        // loaded version and the lost delete is rejected, not silently dropped. The other production write paths
        // (raw EntityManager update, repository.save merge) are pinned on the migrated schema above; this completes
        // the matrix with the delete path, which is only otherwise covered on the Hibernate-generated INTEGER schema.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void deleteCurrentUser_onMigratedSchema_succeeds() {
        // given an up-to-date user loaded from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when a current (non-stale) entity is deleted
        repository.delete(loaded);
        repository.flush();

        // then optimistic locking does not blanket-reject deletes on the BIGINT column: deleting an entity at the
        // row's current version removes the row. This is the delete-path false-positive guard - the counterpart to
        // staleDelete_onMigratedSchema_..., proving the version check only fires on a genuine conflict.
        assertThat(repository.findByUsername(USERNAME)).isEmpty();
    }

    @Test
    void write_afterReReadingConcurrentlyModifiedUser_onMigratedSchema_succeeds() {
        // given a user that a concurrent transaction has since advanced on the migrated schema (version 0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by re-reading the now-current version and writing back
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(reloaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the conflict is recoverable on the real BIGINT column: locking rejects only a behind version, it does
        // not blanket-reject every write. This is the update-path false-positive guard - the recovery counterpart to
        // staleWrite_onMigratedSchema_..., advancing from the reloaded version (1 -> 2) rather than failing.
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 2);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(2L);
    }

    @Test
    void repeatedWrites_onMigratedSchema_incrementVersionMonotonically() {
        // given a freshly persisted user at version 0 on the migrated schema
        repository.saveAndFlush(newUser());

        // when the row is written in two successive write cycles (each reloads a managed instance)
        forceIncrementVersion();
        forceIncrementVersion();

        // then the BIGINT column persists each bump and the version advances monotonically (0 -> 1 -> 2) rather
        // than capping at the first increment - write_onMigratedSchema_... only proves the initial 0 -> 1 bump
        entityManager.clear();
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 2);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(2L);
    }

    @Test
    void write_onMigratedSchema_leavesOtherUsersVersionUnchanged() {
        // given two independently persisted users on the migrated schema, each starting at version 0
        repository.saveAndFlush(newUser());
        repository.saveAndFlush(newUser("other", "other@gmail.com"));

        // when only the first user is written
        forceIncrementVersion();

        // then optimistic locking is scoped per row on the BIGINT column: the untouched user keeps version 0
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
        assertThat(((Number) versionOf("other")).longValue()).isZero();
    }

    @Test
    void read_withoutModification_onMigratedSchema_doesNotChangeVersion() {
        // given a user persisted on the migrated BIGINT column
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when an unmodified entity is flushed
        repository.flush();

        // then no spurious increment is issued against the BIGINT column: a pure read must not bump the version.
        // UserOptimisticLockingTest.read_withoutModification_doesNotChangeVersion proves this on the Hibernate
        // INTEGER column; this is the false-positive guard's missing counterpart on the production schema, where
        // a mistyped/misbound column could otherwise make Hibernate consider the row dirty on every flush.
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    @Test
    void save_unchangedUpToDateUserThroughRepository_onMigratedSchema_succeedsWithoutChangingVersion() {
        // given an up-to-date user detached at version 0 on the migrated schema
        final User detached = repository.saveAndFlush(newUser());
        entityManager.clear();

        // when the unchanged instance is re-saved through the Spring Data merge path
        repository.save(detached);
        repository.flush();

        // then the no-op merge against the BIGINT column neither trips the version check nor spuriously bumps the
        // version - the merge-path false-positive guard on the production schema (the Hibernate-schema counterpart
        // is save_unchangedUpToDateUserThroughRepository_succeedsWithoutChangingVersion)
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    private void forceIncrementVersion() {
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
    }

    private Object versionOf(final String username) {
        return entityManager
                .createNativeQuery("SELECT version FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult();
    }

    private User newUser() {
        return newUser(USERNAME, "email@gmail.com");
    }

    private User newUser(final String username, final String email) {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username(username)
                .email(email)
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        return new User(command, passwordEncoder);
    }
}
