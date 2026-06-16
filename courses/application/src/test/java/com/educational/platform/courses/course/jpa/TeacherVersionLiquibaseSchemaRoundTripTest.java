package com.educational.platform.courses.course.jpa;

import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

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
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies the optimistic-lock {@code @Version} entity round-trips through the <em>real production
 * schema</em> - the one Liquibase builds from {@code db/courses.yml}, where this PR adds the
 * {@code version} column as {@code BIGINT} - rather than the Hibernate-generated schema the other
 * {@code @DataJpaTest} suites use.
 *
 * <p>This closes a seam none of the existing tests cover. The {@code @DataJpaTest} optimistic-locking
 * tests ({@code CourseOptimisticLockingTest}) let Hibernate generate the schema, so the entity's
 * {@code @Version Integer} maps onto an {@code INTEGER} column there - never the production
 * {@code BIGINT}. {@code CoursesLiquibaseMigrationTest} applies the real changelog and asserts the
 * {@code BIGINT} column contract, but only through raw JDBC - it never persists or increments an
 * entity against it. So the deliberate {@code Integer} (entity) &rarr; {@code BIGINT} (column) type
 * choice is exercised on each side in isolation but never end to end. Here we drive the JPA layer
 * (with {@code ddl-auto=none}, matching the production {@code db/courses.yml} setup) against the
 * Liquibase-migrated schema and exercise the {@code @Version} mechanics: init to 0, a versioned
 * increment, and the stale conflict the retrying command handlers recover from - asserting the value
 * lands in, and is matched against, the actual {@code BIGINT} column via a native read.
 *
 * <p>The schema is applied with the raw Liquibase API onto the {@code @DataJpaTest} embedded
 * datasource (the project depends on {@code liquibase-core} only, not Spring Boot's Liquibase
 * auto-configuration, which is also why the sibling migration test drives Liquibase directly).
 * {@code Teacher} is the entity exercised because it maps cleanly onto its changelog table
 * ({@code id} as {@code autoIncrement} matching {@code GenerationType.IDENTITY}, plus {@code username}
 * and {@code version}). {@code Course}'s embedded {@code NumberOfStudents} maps to a {@code number}
 * column that the changelog spells {@code number_of_students}, and {@code CurriculumItem} uses
 * {@code GenerationType.TABLE} whose backing table the changelog does not create; their
 * {@code BIGINT} column contract therefore stays covered by {@code CoursesLiquibaseMigrationTest}.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
class TeacherVersionLiquibaseSchemaRoundTripTest {

    private static final String CHANGELOG = "db/courses.yml";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void applyCoursesChangelog() throws Exception {
        // production builds the schema with Liquibase and runs the entities with ddl-auto=none; mirror
        // that here by migrating the @DataJpaTest embedded datasource with the real changelog. Liquibase
        // tracks applied change sets, so re-running before each test is an idempotent no-op.
        try (Connection migrationConnection = dataSource.getConnection()) {
            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(migrationConnection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    @Test
    void save_teacherAgainstLiquibaseBigintSchema_versionStoredAsZeroInBigintColumn() {
        // given - a new teacher persisted against the Liquibase-produced version BIGINT column
        final Teacher teacher = new Teacher(new CreateTeacherCommand("teacher"));

        // when
        teacherRepository.saveAndFlush(teacher);

        // then - the @Version Integer initialises to 0 and lands in the BIGINT column as 0
        assertThat(ReflectionTestUtils.getField(teacher, "version")).isEqualTo(0);
        assertThat(bigintVersionOf(teacher)).isZero();
    }

    @Test
    void dirtyUpdate_teacherAgainstLiquibaseBigintSchema_versionIncrementsInBigintColumn() {
        // given - a persisted teacher at version 0 in the BIGINT column
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand("teacher")));
        final Integer id = teacher.getId();
        entityManager.clear();

        // when - a dirty update is flushed
        final Teacher loaded = teacherRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(loaded, "username", "renamed");
        teacherRepository.saveAndFlush(loaded);

        // then - the versioned UPDATE binds the Integer version against the BIGINT column and the
        // narrowing Integer<->BIGINT mapping round-trips the advanced value 0 -> 1 back as 1
        assertThat(ReflectionTestUtils.getField(loaded, "version")).isEqualTo(1);
        assertThat(bigintVersionOf(loaded)).isEqualTo(1L);
    }

    @Test
    void saveAndFlush_staleTeacherAgainstLiquibaseBigintSchema_throwsObjectOptimisticLockingFailureException() {
        // given - a persisted teacher at version 0
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand("teacher")));
        final Integer id = teacher.getId();
        entityManager.clear();

        // and - a detached copy snapshotting version 0
        final Teacher stale = teacherRepository.findById(id).orElseThrow();
        entityManager.detach(stale);

        // and - a concurrent update bumps the persisted BIGINT version to 1
        final Teacher concurrent = teacherRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(concurrent, "username", "concurrent");
        teacherRepository.saveAndFlush(concurrent);
        entityManager.clear();

        // when - persisting the now-stale copy against the BIGINT column
        ReflectionTestUtils.setField(stale, "username", "stale");

        // then - the versioned UPDATE matches no row (its version-0 snapshot lost to the concurrent
        // commit) and Spring surfaces the conflict against the production BIGINT column
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> teacherRepository.saveAndFlush(stale));
    }

    private long bigintVersionOf(Teacher teacher) {
        final Object raw = entityManager.getEntityManager()
                .createNativeQuery("SELECT version FROM teacher WHERE id = :id")
                .setParameter("id", teacher.getId())
                .getSingleResult();
        return ((Number) raw).longValue();
    }
}
