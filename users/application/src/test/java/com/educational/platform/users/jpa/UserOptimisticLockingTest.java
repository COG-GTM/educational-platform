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

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    private int idOf(final String username) {
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult()).intValue();
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
