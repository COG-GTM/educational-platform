package com.educational.platform.users;

import com.educational.platform.users.registration.UserRegistrationCommand;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural coverage that the read projections ({@link User#toDTO()} / {@link User#toUserDetails()}) are
 * independent of the {@code @Version} optimistic-locking field on a <em>transient</em> aggregate - one freshly
 * constructed via {@code new User(command, encoder)} but not yet persisted, so its {@code @Version} is still
 * {@code null} (JPA only assigns it on persist).
 *
 * <p>This closes the one cell the rest of the suite leaves open:
 * <ul>
 *   <li>{@link com.educational.platform.users.jpa.UserOptimisticLockingTest#newUser_beforePersist_hasNullVersion()}
 *   pins that the version field <em>is</em> {@code null} before persist, but never drives the projections from that
 *   state.</li>
 *   <li>{@link com.educational.platform.users.jpa.UserPersistenceTest} proves the projections survive a JPA round
 *   trip, but only ever from a <strong>persisted</strong> entity (version 0 or already incremented) - never the
 *   transient, version-{@code null} state the registration handler constructs before {@code repository.save}.</li>
 *   <li>{@link UserVersionMappingTest#userReadModel_doesNotExposeVersion()} pins by reflection that {@link UserDTO}
 *   does not <em>declare</em> a version field; this is its behavioural counterpart - the projections actually
 *   compute correctly while the entity's own version is {@code null}.</li>
 * </ul>
 * A pure, context-free unit test: the projections derive solely from username/email/password/role, so a transient
 * aggregate that has never seen JPA must still project cleanly with a {@code null} version.
 */
class UserTransientProjectionTest {

    private static final String USERNAME = "username";
    private static final String EMAIL = "email@gmail.com";
    private static final String RAW_PASSWORD = "password";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void toDTO_onTransientUser_isUnaffectedByNullVersion() {
        // given a freshly constructed aggregate that JPA has never managed
        final User transientUser = newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT);

        // precondition: the optimistic-lock version is unassigned until persist
        assertThat(transientUser).hasFieldOrPropertyWithValue("version", null);

        // when it is projected to the read DTO
        final UserDTO dto = transientUser.toDTO();

        // then the DTO is fully populated despite the version being null - the projection never reads it
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    @Test
    void toUserDetails_onTransientUser_isUnaffectedByNullVersion() {
        // given a freshly constructed aggregate that JPA has never managed
        final User transientUser = newUser(USERNAME, EMAIL, RoleDTO.ROLE_STUDENT);

        // precondition: the optimistic-lock version is unassigned until persist
        assertThat(transientUser).hasFieldOrPropertyWithValue("version", null);

        // when it is projected to Spring Security UserDetails
        final UserDetails userDetails = transientUser.toUserDetails();

        // then the username, encoded password and granted authority all project with a null version present
        assertThat(userDetails.getUsername()).isEqualTo(USERNAME);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, userDetails.getPassword())).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_STUDENT.getAuthority());
    }

    @Test
    void projections_onTransientTeacher_areUnaffectedByNullVersion() {
        // given a transient teacher (the non-default role) that JPA has never managed
        final User transientTeacher = newUser("teacher", "teacher@gmail.com", RoleDTO.ROLE_TEACHER);

        // precondition: the optimistic-lock version is unassigned until persist
        assertThat(transientTeacher).hasFieldOrPropertyWithValue("version", null);

        // then neither projection leaks the null version, and the non-default role maps through both - completing
        // the role coverage of the transient projection path (the student-role cases above only cover ROLE_STUDENT)
        assertThat(transientTeacher.toDTO().role()).isEqualTo(RoleDTO.ROLE_TEACHER);
        assertThat(transientTeacher.toUserDetails().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.ROLE_TEACHER.getAuthority());
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
