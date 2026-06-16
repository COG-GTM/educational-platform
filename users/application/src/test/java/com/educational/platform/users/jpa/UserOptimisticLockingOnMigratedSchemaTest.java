package com.educational.platform.users.jpa;

import com.educational.platform.users.Role;
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
    void legacyRowInsertedWithoutVersion_onMigratedSchema_loadsAtZeroAndParticipatesInOptimisticLocking() {
        // given a row inserted directly into the migrated table without supplying a version - the shape of a
        // pre-existing/legacy row the upgrade backfills via the column's DEFAULT 0, rather than one the
        // version-aware entity inserted itself (every other test here creates rows through repository.save).
        // On the Hibernate-generated INTEGER schema this cannot even be expressed: that column has no DB-level
        // default, so this DEFAULT-0 backfill behaviour is unique to the migration-owned BIGINT column.
        entityManager.createNativeQuery(
                        "INSERT INTO custom_user (username, email, password, role) VALUES (:username, :email, :password, :role)")
                .setParameter("username", USERNAME)
                .setParameter("email", "email@gmail.com")
                .setParameter("password", "password")
                .setParameter("role", Role.ROLE_STUDENT.ordinal())
                .executeUpdate();
        entityManager.clear();

        // when the backfilled row is loaded through the JPA entity, then written back
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // then the migration's DEFAULT materialises onto the Integer-typed @Version field as 0...
        assertThat(loaded).extracting("version").isInstanceOf(Integer.class);
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 0);

        // ...and the backfilled row is a first-class optimistic-locking participant: a write advances it from the
        // DEFAULT-0 baseline (0 -> 1), proving the entity locks rows it did not itself insert, not just ones it did
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 1);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
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
    void staleMerge_failureIdentifiesConflictingUserAndId_onMigratedSchema() {
        // given a user detached at version 0, with its primary key captured up front for the assertion below
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path against the migrated schema
        // then the translated failure names the conflicting aggregate and its identifier on the real BIGINT column
        // too, not merely that *a* conflict happened. staleMerge_onMigratedSchema_... pins only the exception type
        // here, and UserOptimisticLockingTest.save_staleUserThroughRepository_failureIdentifiesConflictingUserAndId
        // pins this identity payload on the Hibernate-generated INTEGER schema. This is its missing production-schema
        // counterpart: a retyped/misbound version column could change how Hibernate's StaleObjectStateException
        // identifier round-trips through Spring's translation on the only schema that ships.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale))
                .satisfies(thrown -> {
                    assertThat(thrown.getPersistentClassName()).isEqualTo(User.class.getName());
                    assertThat(thrown.getIdentifier()).isEqualTo(id);
                });
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
    void deleteStaleUserAfterRealJpaWrite_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (a request's view captured before a concurrent write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // a concurrent flow performs a real JPA write (forced increment, not a native UPDATE) that bumps the row to 1
        forceIncrementVersion();
        entityManager.clear();

        // when the stale instance is deleted on the migrated schema, the version check finds no row at the loaded
        // version. then a lost delete produced by a genuine application write is rejected on the real BIGINT column
        // too, not just one forged via raw SQL: staleDelete_onMigratedSchema_... and
        // delete_staleUserMultipleVersionsBehind_onMigratedSchema only simulate the conflict with a native UPDATE,
        // and concurrentWriters_firstCommitWins_onMigratedSchema pins the real-write conflict for the merge path
        // only. This is the production-schema, delete-path counterpart to
        // UserOptimisticLockingTest.delete_staleUserAfterRealJpaWrite (Hibernate-generated INTEGER schema).
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> {
                    repository.delete(stale);
                    repository.flush();
                });
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
    void write_afterRefreshingConcurrentlyModifiedUser_onMigratedSchema_succeeds() {
        // given a stale managed instance loaded at version 0, then a concurrent transaction advances the row (0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by refreshing the *same* managed instance in place (re-syncing its version
        // from the row) rather than re-reading a new one via the finder, then writing it back. This is the recovery
        // counterpart to write_afterReReadingConcurrentlyModifiedUser_onMigratedSchema_..., which discards the stale
        // instance and loads a fresh one; refresh re-attaches the existing one against the migrated BIGINT column.
        // UserOptimisticLockingTest.update_afterRefreshingConcurrentlyModifiedUser_succeeds pins this on the
        // Hibernate-generated INTEGER schema only; this is its missing production-schema counterpart.
        entityManager.refresh(stale);
        assertThat(stale).as("refresh re-syncs the stale instance's version from the row")
                .hasFieldOrPropertyWithValue("version", 1);
        entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the refreshed instance is no longer treated as stale: the write succeeds and advances from the
        // refreshed version (1 -> 2) on the real BIGINT column
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);
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

    @Test
    void staleWriteFromNonZeroBaseline_onMigratedSchema_throwsOptimisticLockException() {
        // given a row on the migrated schema whose version has already advanced to 2 before any conflict -
        // every other stale test here loads a freshly persisted version-0 entity
        repository.saveAndFlush(newUser());
        forceIncrementVersion();
        forceIncrementVersion();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);

        // and a concurrent transaction that advances the row past the loaded baseline (2 -> 3)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the raw EntityManager path
        // then the lock check still fires from a non-zero baseline on the BIGINT column: detection compares the
        // loaded version against the row, it is not special-cased to the initial 0 -> 1 transition every other
        // migrated-schema stale test starts from (the production-schema counterpart to
        // UserOptimisticLockingTest.update_staleUserFromNonZeroBaseline_...)
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void staleWriteMultipleVersionsBehind_onMigratedSchema_throwsOptimisticLockException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and two successive concurrent transactions advancing the row two versions ahead (0 -> 2)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 2 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, any divergence (not just an off-by-one) is detected on the
        // BIGINT column too - staleWrite_onMigratedSchema_... only covers the +1 case
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void update_afterConcurrentDelete_onMigratedSchema_throwsOptimisticLockException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and a concurrent transaction that deleted the row out from under us
        entityManager.createNativeQuery("DELETE FROM custom_user WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the now-orphaned instance is written back through the raw EntityManager path the version check finds
        // no matching row. then it surfaces as the JPA-standard exception on the BIGINT column - the lost update is
        // rejected, not silently re-applied. The migrated slice otherwise only covers a version *mismatch*; this
        // pins the distinct no-matching-row path on the production schema.
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void delete_afterConcurrentDelete_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and a concurrent transaction that already deleted the row
        entityManager.createNativeQuery("DELETE FROM custom_user WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when this transaction deletes the same (already removed) row through the Spring Data path the version
        // check matches nothing. then the lost delete is reported as Spring's translated optimistic-locking
        // exception on the BIGINT column, completing the concurrent-delete matrix the migrated slice otherwise omits.
        repository.delete(stale);
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void versionAtIntegerMaxValue_onMigratedSchema_materializesOntoIntegerField() {
        // given a user whose BIGINT row version has been advanced to the top of the Integer range. The migration
        // test proves the column stores values *beyond* Integer.MAX_VALUE at the JDBC level (with no entity in
        // play); this pins the entity-side other half on the production schema - a value at the boundary of the
        // Integer-typed field still materialises intact onto it, rather than truncating the Integer<->BIGINT mapping.
        repository.saveAndFlush(newUser());
        entityManager.createNativeQuery("UPDATE custom_user SET version = :version WHERE username = :username")
                .setParameter("version", Integer.MAX_VALUE)
                .setParameter("username", USERNAME)
                .executeUpdate();
        entityManager.clear();

        // when the entity is read back from the migrated BIGINT column
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();

        // then the maximum representable Integer version round-trips onto the Integer-typed @Version field
        assertThat(reloaded).extracting("version").isInstanceOf(Integer.class);
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", Integer.MAX_VALUE);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo((long) Integer.MAX_VALUE);
    }

    @Test
    void staleWriteFromIntegerMaxValueBaseline_onMigratedSchema_throwsOptimisticLockException() {
        // given a user loaded at the top of the Integer range from the migrated BIGINT column - every other
        // stale test here loads a small (0/2) baseline
        repository.saveAndFlush(newUser());
        entityManager.createNativeQuery("UPDATE custom_user SET version = :version WHERE username = :username")
                .setParameter("version", Integer.MAX_VALUE)
                .setParameter("username", USERNAME)
                .executeUpdate();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(stale).hasFieldOrPropertyWithValue("version", Integer.MAX_VALUE);

        // and a concurrent transaction that advances the row one past Integer.MAX_VALUE - a value only the BIGINT
        // column can hold, so the conflict is detected across the Integer/BIGINT boundary
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, the version check still fires from a max-Integer baseline:
        // detection compares the loaded version against the row and is not limited to small versions
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void concurrentWriters_firstCommitWins_andStaleSecondWriteIsRejected_onMigratedSchema() {
        // given two writers that both loaded the user at version 0 on the migrated schema; the second keeps a
        // detached copy (the state a request holds between read and write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User secondWriter = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(secondWriter);

        // when the first writer commits a real JPA write (forced increment, not a native UPDATE), advancing the row to 1
        forceIncrementVersion();
        entityManager.clear();

        // then the first writer's change is durably in place on the BIGINT column...
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);

        // ...and the now-stale second writer is rejected through the Spring Data merge path rather than silently
        // overwriting it - the PR's headline guarantee (concurrent updates are detected, not lost), proven here on
        // the only schema that ships rather than only on the Hibernate-generated INTEGER column
        // (UserOptimisticLockingTest.concurrentWriters_firstCommitWins...)
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(secondWriter));
    }

    @Test
    void save_detachedUserAtCurrentVersionAfterConcurrentModification_throughRepository_onMigratedSchema_succeeds() {
        // given a user that a concurrent transaction has since advanced on the migrated schema (version 0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by re-reading the now-current version and the unchanged detached instance is
        // written back through the Spring Data merge path - the write path the registration handler actually uses
        final User current = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(current);
        repository.saveAndFlush(current);

        // then the merge succeeds on the BIGINT column: optimistic locking rejects only a behind version, it does not
        // blanket-reject every merge. This is the merge-path false-positive guard on the production schema - the
        // recovery counterpart to staleMerge_onMigratedSchema_..., distinct from the EM/re-read recovery already pinned
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void save_staleUserMultipleVersionsBehindThroughRepository_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 on the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // and two successive concurrent transactions advancing the row two versions ahead (0 -> 2)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 2 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path, any divergence (not just an
        // off-by-one) is rejected on the BIGINT column too - the merge-path multi-behind case the migrated slice
        // otherwise leaves to the EM update path (staleWriteMultipleVersionsBehind_onMigratedSchema_...)
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void save_staleUserFromNonZeroBaseline_throughRepository_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a long-lived row on the migrated schema detached at version 2 - every other merge-path stale test
        // here detaches at version 0 (off-by-one or multi-behind), so the merge path is never exercised from a
        // non-zero baseline on the BIGINT column
        repository.saveAndFlush(newUser());
        forceIncrementVersion();
        forceIncrementVersion();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);

        // and a concurrent transaction that advances the row past the loaded baseline (2 -> 3)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path, the lock check still fires from
        // a non-zero baseline on the BIGINT column: the merge-path detection compares the loaded version against the
        // row and is not special-cased to the initial 0 -> 1 transition. The production-schema, merge-path counterpart
        // to staleWriteFromNonZeroBaseline_onMigratedSchema (raw EntityManager) and
        // UserOptimisticLockingTest.save_staleUserFromNonZeroBaseline_throughRepository (Hibernate schema).
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void save_staleUserAfterConcurrentDelete_throughRepository_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 on the migrated schema (the state a request holds between read and write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // and a concurrent transaction that deleted the row out from under us
        entityManager.createNativeQuery("DELETE FROM custom_user WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path: the set @Version marks it
        // detached, so Hibernate issues an UPDATE (not an INSERT) that matches no row. then the lost update is rejected
        // as a translated optimistic-locking failure - neither silently dropped nor re-inserted as a new row. The
        // migrated slice covers the EM-update and delete paths for a concurrent delete; this pins the merge/save path.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void delete_staleUserMultipleVersionsBehind_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded at version 0 from the migrated schema
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // and two successive concurrent transactions advancing the row two versions ahead (0 -> 2)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 2 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, any divergence (not just an off-by-one) is detected on the BIGINT column
        repository.delete(stale);

        // then the delete path rejects a deleter more than one version behind on the production schema too, mirroring
        // staleDelete_onMigratedSchema_... (which only covers the +1 case)
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void delete_staleUserFromNonZeroBaseline_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a long-lived row on the migrated schema whose version has already advanced to 2 before any
        // conflict - every other delete-path stale test here loads a freshly persisted version-0 entity. The
        // update path (staleWriteFromNonZeroBaseline_onMigratedSchema) and merge/save path
        // (save_staleUserFromNonZeroBaseline_throughRepository_onMigratedSchema) each pin this non-zero baseline
        // on the BIGINT column; the delete path is the missing cell, completing the non-zero-baseline matrix
        // across all three write paths on the production schema (the counterpart to
        // UserOptimisticLockingTest.delete_staleUserFromNonZeroBaseline_...).
        repository.saveAndFlush(newUser());
        forceIncrementVersion();
        forceIncrementVersion();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);

        // and a concurrent transaction that advances the row past the loaded baseline (2 -> 3)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, the version check finds no row at the loaded version
        repository.delete(stale);

        // then the lost delete is rejected from a non-zero baseline on the BIGINT column too: detection compares
        // the loaded version against the row and is not special-cased to the initial 0 -> 1 transition
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void delete_afterReReadingConcurrentlyModifiedUser_onMigratedSchema_succeeds() {
        // given a persisted user that a concurrent transaction has since modified on the migrated schema (0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the entity is re-read (picking up the current version) and then deleted
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        repository.delete(reloaded);
        repository.flush();

        // then the conflict is recoverable on the BIGINT column: deleting the up-to-date reload removes the row. This
        // is the delete-path recovery guard - the counterpart to delete_staleUserMultipleVersionsBehind_... and the
        // delete sibling of write_afterReReadingConcurrentlyModifiedUser_onMigratedSchema_succeeds
        assertThat(repository.findByUsername(USERNAME)).isEmpty();
    }

    @Test
    void delete_afterRefreshingConcurrentlyModifiedUser_onMigratedSchema_succeeds() {
        // given a stale managed instance loaded at version 0, then a concurrent transaction advances the row (0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by refreshing the *same* managed instance in place (re-syncing its version
        // from the row) rather than re-reading a new one via the finder, then deleting it against the migrated BIGINT
        // column. This is the delete-path counterpart to write_afterRefreshingConcurrentlyModifiedUser_onMigratedSchema_succeeds
        // and the refresh-based sibling of delete_afterReReadingConcurrentlyModifiedUser_onMigratedSchema_succeeds (which
        // discards the stale instance and loads a fresh one). UserOptimisticLockingTest.delete_afterRefreshingConcurrentlyModifiedUser_succeeds
        // pins this on the Hibernate-generated INTEGER schema only; this is its missing production-schema counterpart.
        entityManager.refresh(stale);
        assertThat(stale).as("refresh re-syncs the stale instance's version from the row")
                .hasFieldOrPropertyWithValue("version", 1);
        repository.delete(stale);
        repository.flush();

        // then the refreshed instance is no longer treated as stale: the delete succeeds and removes the row on the
        // real BIGINT column
        assertThat(repository.findByUsername(USERNAME)).isEmpty();
    }

    @Test
    void update_staleUserLoadedById_onMigratedSchema_throwsOptimisticLockException() {
        // given a user loaded through the primary-key finder on the migrated schema - the read path a request flow
        // uses (load aggregate by id, then write), which the findByUsername-based migrated-schema tests never exercise
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findById(id).orElseThrow();

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the raw EntityManager path, the version check fails on the
        // BIGINT column exactly as for a findByUsername-loaded entity: optimistic locking is bound to the row, not the
        // finder used to load it (the production-schema counterpart to UserOptimisticLockingTest.update_staleUserLoadedById_...)
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void save_staleUserLoadedById_throughRepository_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded by primary key and detached at version 0 on the migrated schema - the exact read/write
        // shape of a request flow (load the aggregate via the JpaRepository by id, then write back through repository.save)
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findById(id).orElseThrow();
        entityManager.detach(stale);

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path, the conflict surfaces as Spring's
        // translated exception on the BIGINT column just as for a findByUsername-loaded entity, never a silent overwrite
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void delete_staleUserLoadedById_onMigratedSchema_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded through the primary-key finder on the migrated schema - the read shape a request flow
        // uses (load aggregate by id, then delete). update_staleUserLoadedById_onMigratedSchema and
        // save_staleUserLoadedById_throughRepository_onMigratedSchema cover the update/merge paths from a by-id load
        // on the BIGINT column; the delete path from a by-id load is the missing sibling.
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findById(id).orElseThrow();

        // and a concurrent transaction that advanced the row's version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, the version check finds no row at the loaded version
        repository.delete(stale);

        // then the lost delete is rejected on the BIGINT column exactly as for a findByUsername-loaded entity:
        // optimistic locking is bound to the row, not the finder used to load it
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void save_newUser_whenAnotherUsersVersionHasAdvanced_onMigratedSchema_startsAtZero() {
        // given an existing user on the migrated schema whose version has already advanced to 1
        repository.saveAndFlush(newUser());
        forceIncrementVersion();

        // when a brand-new user is subsequently persisted onto the same BIGINT column
        final User other = repository.saveAndFlush(newUser("other", "other@gmail.com"));

        // then version initialization is per-row on the migration's DEFAULT-0 column: the new row starts at 0
        // regardless of the advanced version on the pre-existing row (distinct from write isolation, which only covers
        // rows that were both already at 0). The production-schema counterpart to
        // UserOptimisticLockingTest.save_newUser_whenAnotherUsersVersionHasAdvanced_startsAtZero
        assertThat(other).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf("other")).longValue()).isZero();
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void writes_advanceEachUsersVersionIndependently_onMigratedSchema() {
        // given two independently persisted users on the migrated BIGINT column, each starting at version 0
        repository.saveAndFlush(newUser());
        repository.saveAndFlush(newUser("other", "other@gmail.com"));

        // when the first user is written twice and the second once, in separate write cycles
        forceIncrementVersionOf(USERNAME);
        forceIncrementVersionOf(USERNAME);
        forceIncrementVersionOf("other");

        // then each row carries its own independent counter (2 and 1) on the production schema, proving @Version is
        // per-row rather than a shared/global sequence even against the migration-owned BIGINT column. The
        // production-schema counterpart to UserOptimisticLockingTest.writes_advanceEachUsersVersionIndependently.
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(2L);
        assertThat(((Number) versionOf("other")).longValue()).isEqualTo(1L);
    }

    private int idOf(final String username) {
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult()).intValue();
    }

    private void forceIncrementVersion() {
        forceIncrementVersionOf(USERNAME);
    }

    private void forceIncrementVersionOf(final String username) {
        entityManager.clear();
        final User loaded = repository.findByUsername(username).orElseThrow();
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
