package com.educational.platform.users.security;

import com.educational.platform.users.Role;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.User;
import com.educational.platform.users.UserRepository;
import com.educational.platform.users.registration.UserRegistrationCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Regression coverage driving the authentication read path - the {@link MyUserDetails}
 * {@link org.springframework.security.core.userdetails.UserDetailsService} Spring Security invokes on every
 * sign in - against a database, so the {@code @Version} optimistic-locking field added to {@link User} is
 * exercised through the actual production lookup rather than a direct {@link User#toUserDetails()} call.
 *
 * <p>{@link com.educational.platform.users.registration.UserRegistrationVersioningTest} pins the write path
 * (a registered aggregate is persisted with a JPA-initialised {@code @Version}); this pins the symmetric read
 * path: loading a persisted {@code @Version}-bearing user through {@code MyUserDetails} must keep yielding the
 * same {@link UserDetails} projection, both at the initial version and after the version has advanced.
 * {@link com.educational.platform.users.jpa.UserPersistenceTest} only calls {@code toUserDetails()} directly,
 * so the production {@code UserDetailsService} seam is otherwise untested.
 */
@DataJpaTest
class MyUserDetailsVersioningTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String RAW_PASSWORD = "password";

    @Autowired
    private UserRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void loadUserByUsername_persistedUser_returnsUserDetailsUnaffectedByVersion() {
        // given a user persisted with the new @Version column in place
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        entityManager.clear();

        // when Spring Security loads it through the production UserDetailsService
        final UserDetails userDetails = new MyUserDetails(repository).loadUserByUsername(USERNAME);

        // then the username, encoded password and authority survive the round trip - the version is not leaked in
        assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_STUDENT.getAuthority());
    }

    @Test
    void loadUserByUsername_afterVersionIncrement_stillReturnsCorrectUserDetails() {
        // given a persisted user whose version has since been bumped to 1
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // when the user is loaded for authentication at a non-zero version
        final UserDetails userDetails = new MyUserDetails(repository).loadUserByUsername(USERNAME);

        // then a non-zero version never disturbs the login projection
        // (the persistence test only exercises version 0 through this read path)
        assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_STUDENT.getAuthority());
    }

    @Test
    void loadUserByUsername_teacher_returnsTeacherAuthorityUnaffectedByVersion() {
        // given a teacher persisted with the new @Version column in place
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        entityManager.clear();

        // when loaded through the production UserDetailsService
        final UserDetails userDetails = new MyUserDetails(repository).loadUserByUsername("teacher");

        // then the non-default role maps through the login read path unchanged by @Version
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_TEACHER.getAuthority());
    }

    @Test
    void loadUserByUsername_teacher_afterVersionIncrement_stillReturnsTeacherAuthority() {
        // given a teacher whose version has since been bumped to 1
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        entityManager.clear();
        final User loaded = repository.findByUsername("teacher").orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // when the teacher is loaded for authentication at a non-zero version
        final UserDetails userDetails = new MyUserDetails(repository).loadUserByUsername("teacher");

        // then the non-default role still maps through the login read path at a non-zero version: completes the
        // role x version matrix (loadUserByUsername_afterVersionIncrement_... only covers the student role, and
        // loadUserByUsername_teacher_... only covers version 0)
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_TEACHER.getAuthority());
    }

    @Test
    void loadUserByUsername_doesNotAdvanceUserVersion() {
        // given a user persisted at version 0
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        entityManager.clear();

        // when Spring Security loads it through the production UserDetailsService (a pure read)
        new MyUserDetails(repository).loadUserByUsername(USERNAME);

        // force a genuine DB round trip so the version is read back from the row, not the in-context instance
        entityManager.flush();
        entityManager.clear();

        // then loading a user for authentication does not advance the optimistic-lock version: the
        // UserDetailsService read path is side-effect-free under @Version, so authenticating never churns the
        // version (which would otherwise cause spurious lock contention or needless writes). The projection tests
        // pin *what* the load yields; this pins that the load only reads.
        // UserOptimisticLockingTest.read_withoutModification_doesNotChangeVersion proves this at the entity level
        // (a bare finder + flush); this proves it through the production UserDetailsService seam.
        assertThat(repository.findByUsername(USERNAME).orElseThrow()).hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFoundException() {
        // given a populated table that carries the new version column
        repository.saveAndFlush(newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT));
        entityManager.clear();

        // when an absent username is looked up
        // then adding @Version leaves the not-found contract intact: the lookup still distinguishes a missing
        // user rather than returning a stale or empty UserDetails
        assertThatExceptionOfType(UsernameNotFoundException.class)
                .isThrownBy(() -> new MyUserDetails(repository).loadUserByUsername("missing"));
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
