package com.educational.platform.users.jpa;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
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
 * Verifies JPA optimistic locking enabled by the {@code @Version} field on {@link User}.
 */
@DataJpaTest
public class UserOptimisticLockingTest {

    private static final String USERNAME = "username";

    @Autowired
    private UserRepository repository;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void schemaUnderTest_isTheHibernateGeneratedIntegerColumn_notTheMigratedBigInt() throws SQLException {
        // precondition that gives every other test here its meaning: the default @DataJpaTest slice runs the
        // entity against the INTEGER version column Hibernate derives from the Integer @Version field
        // (ddl-auto=create-drop), not the migration's BIGINT column. This is the mirror of
        // UserOptimisticLockingOnMigratedSchemaTest.schemaUnderTest_isTheMigratedBigIntColumn_notAHibernateGeneratedOne:
        // together they pin that the two locking suites genuinely exercise different column types rather than
        // silently collapsing onto the same schema (e.g. were the migration accidentally applied in this slice).
        try (Connection connection = dataSource.getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, "CUSTOM_USER", "VERSION")) {
            assertThat(columns.next()).as("version column exists on the Hibernate-generated custom_user").isTrue();
            assertThat(columns.getInt("DATA_TYPE"))
                    .as("version is the INTEGER Hibernate derives from the Integer field, not the migration's BIGINT")
                    .isEqualTo(Types.INTEGER)
                    .isNotEqualTo(Types.BIGINT);
        }
    }

    @Test
    void newUser_beforePersist_hasNullVersion() {
        // given / when
        final User user = newUser();

        // then the version is unmanaged until JPA persists the entity
        assertThat(user).hasFieldOrPropertyWithValue("version", null);
    }

    @Test
    void save_newUser_initializesVersionToZero() {
        // given
        repository.saveAndFlush(newUser());

        // when
        final Object version = versionOf(USERNAME);

        // then
        assertThat(((Number) version).longValue()).isZero();
    }

    @Test
    void save_newUser_populatesManagedEntityVersionToZero() {
        // given / when a transient user is persisted
        final User saved = repository.saveAndFlush(newUser());

        // then JPA assigns version 0 on the managed in-memory instance, not only on the row
        // (complements save_newUser_initializesVersionToZero, which asserts the persisted column)
        assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    void save_newUser_managedVersionIsIntegerTyped() {
        // given / when a transient user is persisted
        final User saved = repository.saveAndFlush(newUser());

        // then the managed version is an Integer: the @Version field is deliberately typed Integer while the
        // backing column is BIGINT, so this pins the entity-side type and guards the intentional decoupling
        assertThat(saved).extracting("version").isInstanceOf(Integer.class);
    }

    @Test
    void reloadedUser_exposesIntegerTypedVersion() {
        // given a persisted user, with the in-memory instance evicted so the next read is a genuine DB round trip
        repository.saveAndFlush(newUser());
        entityManager.clear();

        // when the entity is read back from the database into a fresh persistence context
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();

        // then the version materialised from the (BIGINT) column maps back to the Integer-typed @Version field.
        // save_newUser_managedVersionIsIntegerTyped only inspects the instance returned by saveAndFlush, which
        // JPA populates directly without ever reading the column - this pins the column -> field read mapping,
        // the other half of the intentional Integer<->BIGINT decoupling.
        assertThat(reloaded).extracting("version").isInstanceOf(Integer.class);
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    void write_managedUser_incrementsVersion() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when the row is written (forced increment exercises the @Version column)
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the JPA-managed version is bumped on both the entity and the row
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 1);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void write_oneUser_leavesOtherUsersVersionUnchanged() {
        // given two independently persisted users, each starting at version 0
        repository.saveAndFlush(newUser());
        repository.saveAndFlush(newUser("other", "other@gmail.com"));

        // when only the first user is written
        forceIncrementVersion();

        // then optimistic locking is scoped per row: the untouched user keeps version 0
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
        assertThat(((Number) versionOf("other")).longValue()).isZero();
    }

    @Test
    void save_newUser_whenAnotherUsersVersionHasAdvanced_startsAtZero() {
        // given an existing user whose version has already advanced to 1
        repository.saveAndFlush(newUser());
        forceIncrementVersion();

        // when a brand-new user is subsequently persisted
        final User other = repository.saveAndFlush(newUser("other", "other@gmail.com"));

        // then version initialization is per-row: the new row starts at 0 regardless of the
        // advanced version on the pre-existing row (distinct from write isolation, which only
        // covers rows that were both already at 0 before one was written)
        assertThat(other).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf("other")).longValue()).isZero();
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void write_managedUser_multipleTimes_incrementsVersionEachTime() {
        // given a freshly persisted user at version 0
        repository.saveAndFlush(newUser());

        // when the row is written in two successive write cycles (each reloads a managed instance)
        forceIncrementVersion();
        forceIncrementVersion();

        // then the version advances monotonically (0 -> 1 -> 2) rather than capping at the first bump
        entityManager.clear();
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 2);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(2L);
    }

    @Test
    void incrementedVersion_isVisibleAfterReload() {
        // given a persisted user whose version has been bumped to 1
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // when the persistence context is cleared and the entity read back
        entityManager.clear();
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();

        // then a freshly loaded entity exposes the persisted, incremented version
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 1);
    }

    @Test
    void read_withoutModification_doesNotChangeVersion() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when an unmodified entity is flushed
        repository.flush();

        // then no spurious version increment happens
        assertThat(loaded).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    @Test
    void update_staleUserAfterConcurrentModification_throwsOptimisticLockException() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, the version check fails
        // then (raw EntityManager write surfaces the JPA-standard exception)
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void update_staleUserMultipleVersionsBehind_throwsOptimisticLockException() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate two successive concurrent transactions, advancing the row two versions ahead (0 -> 2)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 2 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, any divergence (not just an off-by-one) is detected
        // then
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void update_staleUserFromNonZeroBaseline_throwsOptimisticLockException() {
        // given a long-lived row whose version has already advanced to 2 before any conflict - unlike every
        // other stale-write test, which loads a freshly persisted version-0 entity
        repository.saveAndFlush(newUser());
        forceIncrementVersion();
        forceIncrementVersion();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);

        // when a concurrent transaction advances the row past the loaded baseline (2 -> 3)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, the version check still fires from a non-zero baseline:
        // then optimistic locking compares the loaded version against the row, it is not special-cased to the
        // initial 0 -> 1 transition the other stale-write tests all start from
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void staleUser_holdsOutdatedVersion_afterConcurrentModification() {
        // given a persisted user loaded into the context at version 0
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // when a concurrent transaction advances the row's version to 1
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // then the in-context instance still reports the version it was loaded with while the row has moved on
        // - precisely the divergence the next write must detect
        assertThat(stale).hasFieldOrPropertyWithValue("version", 0);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void update_afterReloadingConcurrentlyModifiedUser_succeeds() {
        // given a persisted user that a concurrent transaction has since modified (version 0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the entity is re-read (picking up the current version) and then written back
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(reloaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the conflict is recoverable: the write succeeds and advances from the reloaded version (1 -> 2)
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 2);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(2L);
    }

    @Test
    void update_afterRefreshingConcurrentlyModifiedUser_succeeds() {
        // given a stale managed instance: loaded at version 0, then a concurrent transaction advances the row (0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by refreshing the *same* managed instance in place (re-syncing its
        // version from the row) rather than re-reading a new instance via the finder, then writing it back.
        // This is the recovery counterpart to update_afterReloadingConcurrentlyModifiedUser_succeeds, which
        // discards the stale instance and loads a fresh one; refresh re-attaches the existing one.
        entityManager.refresh(stale);
        assertThat(stale).as("refresh re-syncs the stale instance's version from the row")
                .hasFieldOrPropertyWithValue("version", 1);
        entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();

        // then the refreshed instance is no longer treated as stale: the write succeeds and advances from the
        // refreshed version (1 -> 2)
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(2L);
    }

    @Test
    void save_staleUserThroughRepository_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (the state a request holds between read and write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path (repository.save)
        // then the version check fails with Spring's translated optimistic-locking exception, not a silent overwrite
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void save_staleUserThroughRepository_failureIdentifiesConflictingUserAndId() {
        // given a user detached at version 0, with its primary key captured up front for the assertion below
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path
        // then the translated failure does not merely signal that *a* conflict happened: it names the conflicting
        // aggregate and its identifier - the information a caller needs to report which User lost the race.
        // save_staleUserThroughRepository_throwsObjectOptimisticLockingFailureException pins only the exception
        // type; this pins the identity payload Spring carries over from Hibernate's StaleObjectStateException.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale))
                .satisfies(thrown -> {
                    assertThat(thrown.getPersistentClassName()).isEqualTo(User.class.getName());
                    assertThat(thrown.getIdentifier()).isEqualTo(id);
                });
    }

    @Test
    void delete_staleUserThroughRepository_failureIdentifiesConflictingUserAndId() {
        // given a user loaded at version 0, with its primary key captured up front for the assertion below
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, the version check finds no row at the loaded version
        repository.delete(stale);

        // then a lost delete does not merely signal that *a* conflict happened: it names the conflicting aggregate
        // and its identifier - the information a caller needs to report which User lost the race. The delete-path
        // conflict tests (delete_staleUserAfterConcurrentModification_..., delete_afterConcurrentDelete_...) pin only
        // the exception type, and save_staleUserThroughRepository_failureIdentifiesConflictingUserAndId pins this
        // identity payload on the merge/save path. A lost delete is a distinct Hibernate action carrying its own
        // StaleObjectStateException, so this pins that Spring also carries the identity over for the delete path.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush)
                .satisfies(thrown -> {
                    assertThat(thrown.getPersistentClassName()).isEqualTo(User.class.getName());
                    assertThat(thrown.getIdentifier()).isEqualTo(id);
                });
    }

    @Test
    void save_staleUserAfterRealJpaWrite_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (a request's view captured before a concurrent write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // a concurrent flow performs a real JPA write (forced increment, not a native UPDATE) that bumps the row to 1
        forceIncrementVersion();
        entityManager.clear();

        // when the stale instance is written back through the Spring Data merge path
        // then a conflict produced by a genuine application write is detected too, not just one forged via raw SQL
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void concurrentWriters_firstCommitWins_andStaleSecondWriteIsRejectedNotSilentlyApplied() {
        // given two writers that both loaded the user at version 0; the second keeps a detached copy
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User secondWriter = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(secondWriter);

        // when the first writer commits a real JPA write, advancing the persisted row to version 1
        forceIncrementVersion();
        entityManager.clear();

        // then the first writer's change is durably in place before the second writer acts...
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);

        // ...and the second, now-stale writer is rejected rather than silently overwriting it - this is
        // the headline guarantee of the PR: concurrent updates are detected, not lost. Every other test
        // asserts either the conflict or the surviving state; this pins both halves in one flow.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(secondWriter));
    }

    @Test
    void save_staleUserMultipleVersionsBehindThroughRepository_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // simulate two successive concurrent transactions, advancing the row two versions ahead (0 -> 2)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 2 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path
        // then any divergence (not just an off-by-one) is rejected, complementing the +1 merge-path case
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void save_staleUserFromNonZeroBaseline_throughRepository_throwsObjectOptimisticLockingFailureException() {
        // given a long-lived row detached at version 2 - every other merge-path stale test detaches at the
        // initial version 0 (off-by-one or multi-behind), so the merge path is never exercised from a non-zero baseline
        repository.saveAndFlush(newUser());
        forceIncrementVersion();
        forceIncrementVersion();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);

        // when a concurrent transaction advances the row past the loaded baseline (2 -> 3)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path, the version check still fires
        // then optimistic locking on the merge path compares the loaded version against the row; like the raw-EM
        // counterpart (update_staleUserFromNonZeroBaseline) it is not special-cased to the initial 0 -> 1 transition
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void save_unchangedUpToDateUserThroughRepository_succeedsWithoutChangingVersion() {
        // given an up-to-date user detached at version 0
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User detached = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.clear();

        // when the unchanged instance is re-saved through the Spring Data merge path
        repository.save(detached);
        repository.flush();

        // then the no-op merge neither trips the version check nor spuriously bumps the version
        assertThat(((Number) versionOf(USERNAME)).longValue()).isZero();
    }

    @Test
    void save_detachedUserAtCurrentVersionAfterConcurrentModification_throughRepository_succeeds() {
        // given a user that a concurrent transaction has since advanced (version 0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by re-reading the now-current version and the unchanged detached instance
        // is written back through the Spring Data merge path - the write path the registration handler uses
        final User current = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(current);
        repository.saveAndFlush(current);

        // then the merge succeeds: optimistic locking rejects only a behind version, it does not blanket-reject
        // every merge. This is the recovery counterpart to save_staleUserThroughRepository_..., and unlike
        // save_unchangedUpToDateUserThroughRepository_... it starts from a non-zero version produced by a genuine
        // concurrent modification - the no-op merge neither trips the version check nor spuriously bumps the row.
        assertThat(((Number) versionOf(USERNAME)).longValue()).isEqualTo(1L);
    }

    @Test
    void delete_currentUser_succeeds() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();

        // when
        repository.delete(loaded);
        repository.flush();

        // then
        assertThat(repository.findByUsername(USERNAME)).isEmpty();
    }

    @Test
    void delete_staleUserAfterConcurrentModification_throwsOptimisticLockException() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, the version check fails
        repository.delete(stale);

        // then
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void delete_staleUserAfterRealJpaWrite_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (a request's view captured before a concurrent write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // a concurrent flow performs a real JPA write (forced increment, not a native UPDATE) that bumps the row to 1
        forceIncrementVersion();
        entityManager.clear();

        // when the stale instance is deleted through the Spring Data merge path
        // then a lost-delete produced by a genuine application write is rejected too, not just one forged via raw SQL
        // (the delete-path sibling of save_staleUserAfterRealJpaWrite)
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> {
                    repository.delete(stale);
                    repository.flush();
                });
    }

    @Test
    void save_staleUserAfterConcurrentDelete_throughRepository_throwsObjectOptimisticLockingFailureException() {
        // given a user detached at version 0 (the state a request holds between read and write)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.detach(stale);

        // simulate a concurrent transaction that deleted the row out from under us
        entityManager.createNativeQuery("DELETE FROM custom_user WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path (repository.save):
        // the set @Version marks it detached, so Hibernate issues an UPDATE (not an INSERT) that matches no row.
        // then the lost update is rejected as a translated optimistic-locking failure - it is neither silently
        // dropped nor re-inserted as a new row. update_afterConcurrentDelete covers this for the raw-EM update
        // path and delete_afterConcurrentDelete for the delete path; this pins the merge/save update path.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void delete_staleUserMultipleVersionsBehind_throwsObjectOptimisticLockingFailureException() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate two successive concurrent transactions, advancing the row two versions ahead (0 -> 2)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 2 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, any divergence (not just an off-by-one) is detected
        repository.delete(stale);

        // then the delete path rejects a deleter more than one version behind, mirroring the update-path case
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void delete_staleUserFromNonZeroBaseline_throwsObjectOptimisticLockingFailureException() {
        // given a long-lived row whose version has already advanced to 2 before any conflict - every other
        // delete-path stale test loads a freshly persisted version-0 entity. The update path
        // (update_staleUserFromNonZeroBaseline) and the merge/save path
        // (save_staleUserFromNonZeroBaseline_throughRepository) each pin this non-zero baseline; the delete path
        // is the missing cell, so this completes the non-zero-baseline coverage across all three write paths.
        repository.saveAndFlush(newUser());
        forceIncrementVersion();
        forceIncrementVersion();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(stale).hasFieldOrPropertyWithValue("version", 2);

        // when a concurrent transaction advances the row past the loaded baseline (2 -> 3)
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, the version check finds no row at the loaded version
        repository.delete(stale);

        // then the lost delete is rejected from a non-zero baseline too: optimistic locking compares the loaded
        // version against the row, it is not special-cased to the initial 0 -> 1 transition the other delete-path
        // stale tests all start from
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void update_afterConcurrentDelete_throwsOptimisticLockException() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate a concurrent transaction that deleted the row out from under us
        entityManager.createNativeQuery("DELETE FROM custom_user WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the now-orphaned instance is written back, the version check finds no matching row
        // then (raw EntityManager write surfaces the JPA-standard exception, just like a stale version would)
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void delete_afterConcurrentDelete_throwsObjectOptimisticLockingFailureException() {
        // given
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();

        // simulate a concurrent transaction that already deleted the row
        entityManager.createNativeQuery("DELETE FROM custom_user WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when this transaction deletes the same (already removed) row, the version check matches nothing
        repository.delete(stale);

        // then the lost-delete is reported through Spring's translated optimistic-locking exception
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void delete_afterReloadingConcurrentlyModifiedUser_succeeds() {
        // given a persisted user that a concurrent transaction has since modified (version 0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the entity is re-read (picking up the current version) and then deleted
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        repository.delete(reloaded);
        repository.flush();

        // then the conflict is recoverable: deleting the up-to-date reload removes the row
        assertThat(repository.findByUsername(USERNAME)).isEmpty();
    }

    @Test
    void delete_afterRefreshingConcurrentlyModifiedUser_succeeds() {
        // given a stale managed instance: loaded at version 0, then a concurrent transaction advances the row (0 -> 1)
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the conflict is resolved by refreshing the *same* managed instance in place (re-syncing its version
        // from the row) rather than re-reading a new instance via the finder, then deleting it. This is the
        // delete-path counterpart to update_afterRefreshingConcurrentlyModifiedUser_succeeds and the refresh-based
        // sibling of delete_afterReloadingConcurrentlyModifiedUser_succeeds (which discards the stale instance and
        // loads a fresh one); refresh re-attaches the existing one.
        entityManager.refresh(stale);
        assertThat(stale).as("refresh re-syncs the stale instance's version from the row")
                .hasFieldOrPropertyWithValue("version", 1);
        repository.delete(stale);
        repository.flush();

        // then the refreshed instance is no longer treated as stale: the delete is not rejected and the row is removed
        assertThat(repository.findByUsername(USERNAME)).isEmpty();
    }

    @Test
    void update_staleUserLoadedById_throwsOptimisticLockException() {
        // given a user loaded through the primary-key finder - the read path a request flow uses
        // (load aggregate by id, then write), which the findByUsername-based tests never exercise
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findById(id).orElseThrow();

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, the version check fails exactly as for a
        // findByUsername-loaded entity: optimistic locking is bound to the row, not the finder used
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void save_staleUserLoadedById_throughRepository_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded by primary key and detached at version 0 - the exact read/write shape of a
        // request flow (load the aggregate via the JpaRepository by id, then write back through repository.save).
        // The merge-path stale tests above all load via findByUsername, and the only findById stale test drives
        // the raw EntityManager; this pins the findById + Spring Data merge combination.
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findById(id).orElseThrow();
        entityManager.detach(stale);

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back through the Spring Data merge path
        // then optimistic locking is bound to the row, not the finder: the conflict surfaces as Spring's
        // translated exception just as for a findByUsername-loaded entity, never a silent overwrite
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> repository.saveAndFlush(stale));
    }

    @Test
    void delete_staleUserLoadedById_throwsObjectOptimisticLockingFailureException() {
        // given a user loaded through the primary-key finder - the read shape a request flow uses (load aggregate
        // by id, then delete). update_staleUserLoadedById and save_staleUserLoadedById_throughRepository cover the
        // update/merge paths from a by-id load; the delete path from a by-id load is the missing sibling.
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User stale = repository.findById(id).orElseThrow();

        // simulate a concurrent transaction that updated the row and bumped its version
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is deleted, the version check finds no row at the loaded version
        repository.delete(stale);

        // then the lost delete is rejected exactly as for a findByUsername-loaded entity: optimistic locking is
        // bound to the row, not the finder used to load it
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(repository::flush);
    }

    @Test
    void reloadedUser_atIntegerMaxValueVersion_materializesOntoIntegerField() {
        // given a persisted user whose row version has been advanced to the top of the Integer range. Every
        // other test here exercises only small versions (0/1/2); this pins that the column -> field read holds
        // at the boundary of the @Version field's own type, guarding against a narrowing/truncation regression.
        repository.saveAndFlush(newUser());
        entityManager.createNativeQuery("UPDATE custom_user SET version = :version WHERE username = :username")
                .setParameter("version", Integer.MAX_VALUE)
                .setParameter("username", USERNAME)
                .executeUpdate();
        entityManager.clear();

        // when the entity is read back from the database
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();

        // then the maximum representable Integer version materialises intact onto the Integer-typed @Version field
        assertThat(reloaded).extracting("version").isInstanceOf(Integer.class);
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", Integer.MAX_VALUE);
    }

    @Test
    void update_staleUserNearIntegerMaxValueBaseline_throwsOptimisticLockException() {
        // given a user loaded at the top of the INTEGER column's range; every other stale test here loads a
        // small (0/2) baseline
        repository.saveAndFlush(newUser());
        entityManager.createNativeQuery("UPDATE custom_user SET version = :version WHERE username = :username")
                .setParameter("version", Integer.MAX_VALUE - 1)
                .setParameter("username", USERNAME)
                .executeUpdate();
        entityManager.clear();
        final User stale = repository.findByUsername(USERNAME).orElseThrow();
        assertThat(stale).hasFieldOrPropertyWithValue("version", Integer.MAX_VALUE - 1);

        // and a concurrent transaction advances the row to Integer.MAX_VALUE - the largest value this INTEGER
        // column can hold. This is the Hibernate-INTEGER-schema counterpart to the migrated suite's
        // staleWriteFromIntegerMaxValueBaseline_onMigratedSchema, which advances one *past* Integer.MAX_VALUE (a
        // value only the BIGINT column can store); here the conflict is pinned at the boundary the INTEGER column
        // can actually represent.
        entityManager.createNativeQuery("UPDATE custom_user SET version = version + 1 WHERE username = :username")
                .setParameter("username", USERNAME)
                .executeUpdate();

        // when the stale instance is written back, the version check still fires from a near-max-Integer baseline:
        // detection compares the loaded version against the row and is not limited to small versions
        assertThatExceptionOfType(OptimisticLockException.class)
                .isThrownBy(() -> {
                    entityManager.lock(stale, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
                    repository.flush();
                });
    }

    @Test
    void writes_advanceEachUsersVersionIndependently() {
        // given two independently persisted users, each starting at version 0
        repository.saveAndFlush(newUser());
        repository.saveAndFlush(newUser("other", "other@gmail.com"));

        // when the first user is written twice and the second once, in separate write cycles
        forceIncrementVersionOf(USERNAME);
        forceIncrementVersionOf(USERNAME);
        forceIncrementVersionOf("other");

        // then each row carries its own independent counter (2 and 1), proving @Version is per-row rather than a
        // shared/global sequence. write_oneUser_leavesOtherUsersVersionUnchanged only ever advances a single row
        // (the other staying at 0); this pins two rows held at *different* non-zero versions simultaneously.
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
