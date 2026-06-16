package com.educational.platform.users.jpa;

import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.EntityManager;
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
    void save_newUser_initializesVersionToZero() {
        // given
        repository.saveAndFlush(newUser());

        // when
        final Object version = versionOf(USERNAME);

        // then
        assertThat(((Number) version).longValue()).isZero();
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

    private Object versionOf(final String username) {
        return entityManager
                .createNativeQuery("SELECT version FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult();
    }

    private User newUser() {
        final UserRegistrationCommand command = UserRegistrationCommand.builder()
                .username(USERNAME)
                .email("email@gmail.com")
                .password("password")
                .role(RoleDTO.ROLE_STUDENT)
                .build();
        return new User(command, passwordEncoder);
    }
}
