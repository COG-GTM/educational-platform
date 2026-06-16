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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage ensuring the {@code @Version} field added to {@link User} does not disturb the
 * entity's existing persistence contract: a persisted user must still round-trip through JPA and expose
 * the same read projections ({@link User#toDTO()} / {@link User#toUserDetails()}) and finder behaviour.
 */
@DataJpaTest
public class UserPersistenceTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String RAW_PASSWORD = "password";

    @Autowired
    private UserRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void persistAndReload_preservesDtoProjection() {
        // given a user persisted with the new @Version column in place
        repository.saveAndFlush(newUser());
        entityManager.clear();

        // when it is read back from the database
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();

        // then the DTO projection is unaffected by the version field
        final UserDTO dto = reloaded.toDTO();
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void persistAndReload_preservesUserDetailsProjection() {
        // given a user persisted with the new @Version column in place
        repository.saveAndFlush(newUser());
        entityManager.clear();

        // when it is read back and projected to Spring Security UserDetails
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();
        final UserDetails userDetails = reloaded.toUserDetails();

        // then the username, encoded password and granted authority survive the round trip
        assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_STUDENT.getAuthority());
    }

    @Test
    void persistAndReload_preservesTeacherRoleProjection() {
        // given a teacher persisted with the new @Version column in place
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        entityManager.clear();

        // when it is read back from the database
        final User reloaded = repository.findByUsername("teacher").orElseThrow();

        // then the version field does not interfere with mapping the non-default role through either projection
        assertThat(reloaded.toDTO().role()).isEqualTo(RoleDTO.ROLE_TEACHER);
        assertThat(reloaded.toUserDetails().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_TEACHER.getAuthority());
    }

    @Test
    void projections_afterVersionIncrement_areUnaffected() {
        // given a persisted user whose version has since been bumped to 1
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // when the entity is read back at its incremented version and projected
        final User reloaded = repository.findByUsername(USERNAME).orElseThrow();

        // then a changed version never leaks into the read projections
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
    }

    @Test
    void projections_afterTeacherVersionIncrement_areUnaffected() {
        // given a teacher whose version has since been bumped to 1
        repository.saveAndFlush(newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER));
        entityManager.clear();
        final User loaded = repository.findByUsername("teacher").orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // when the teacher is read back at its incremented version and projected
        final User reloaded = repository.findByUsername("teacher").orElseThrow();

        // then a changed version never leaks into either read projection for the non-default role: completes the
        // role x version matrix of the direct projection path (projections_afterVersionIncrement_areUnaffected only
        // covers the student role, and persistAndReload_preservesTeacherRoleProjection only covers version 0)
        assertThat(reloaded.toDTO().role()).isEqualTo(RoleDTO.ROLE_TEACHER);
        final UserDetails userDetails = reloaded.toUserDetails();
        assertThat(userDetails.getUsername()).isEqualTo("teacher");
        assertThat(passwordEncoder.matches(RAW_PASSWORD, userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_TEACHER.getAuthority());
    }

    @Test
    void existsByUsername_reflectsPersistedAndAbsentUsers() {
        // given a single persisted user
        repository.saveAndFlush(newUser());

        // then existence is reported for the persisted username and denied for an unknown one
        assertThat(repository.existsByUsername(USERNAME)).isTrue();
        assertThat(repository.existsByUsername("missing")).isFalse();
    }

    @Test
    void existsByUsername_afterVersionIncrement_stillReportsTrue() {
        // given a persisted user whose version has since been bumped to 1
        repository.saveAndFlush(newUser());
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // then the derived existence query keeps working once the version is non-zero
        // (existsByUsername_reflectsPersistedAndAbsentUsers only exercises version 0)
        assertThat(repository.existsByUsername(USERNAME)).isTrue();
    }

    @Test
    void persist_assignsGeneratedIdAlongsideVersion() {
        // given / when a transient user is persisted
        repository.saveAndFlush(newUser());

        // then introducing @Version next to @Id leaves identity generation intact:
        // the row still receives a generated, positive primary key
        assertThat(idOf(USERNAME)).isPositive();
    }

    @Test
    void findById_afterPersist_preservesProjectionsAndAssignsVersionZero() {
        // given a user persisted with the new @Version column in place
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();

        // when it is read back through the primary-key finder - the read path request flows use,
        // and the only one not exercised by the findByUsername-based persistence tests
        final User reloaded = repository.findById(id).orElseThrow();

        // then loading by id is unaffected by the new @Version/@Id pairing: both projections
        // round-trip and a freshly persisted row reports version 0
        final UserDTO dto = reloaded.toDTO();
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(reloaded.toUserDetails().getUsername()).isEqualTo(USERNAME);
        assertThat(reloaded).hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    void findById_afterVersionIncrement_preservesProjectionsAtNonZeroVersion() {
        // given a persisted user whose version has since been bumped to 1
        repository.saveAndFlush(newUser());
        final int id = idOf(USERNAME);
        entityManager.clear();
        final User loaded = repository.findByUsername(USERNAME).orElseThrow();
        entityManager.lock(loaded, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        repository.flush();
        entityManager.clear();

        // when the entity is read back through the primary-key finder at its incremented version
        final User reloaded = repository.findById(id).orElseThrow();

        // then the by-id read path keeps projecting correctly once the version is non-zero. findById is otherwise
        // only pinned at version 0 (findById_afterPersist_preservesProjectionsAndAssignsVersionZero), while the
        // after-increment cell is covered for the findByUsername projections (projections_afterVersionIncrement_areUnaffected)
        // and for the derived existence query (existsByUsername_afterVersionIncrement_stillReportsTrue) but not for
        // findById. This closes that cell: an advanced version never leaks into the by-id read projections.
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

    private int idOf(final String username) {
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM custom_user WHERE username = :username")
                .setParameter("username", username)
                .getSingleResult()).intValue();
    }

    private User newUser() {
        return newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT);
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
