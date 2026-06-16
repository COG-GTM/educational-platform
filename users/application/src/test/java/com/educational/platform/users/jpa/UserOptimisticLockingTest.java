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
