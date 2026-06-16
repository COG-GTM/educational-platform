package com.educational.platform.courses.course.jpa;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurriculumItem;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies the JPA optimistic locking ({@code @Version}) behaviour added to {@link CurriculumItem}.
 *
 * <p>Unlike {@code Course} and {@code Teacher}, a {@code CurriculumItem} cannot be inserted against
 * the Hibernate auto-generated {@code @DataJpaTest} schema out of the box: the single-table
 * inheritance discriminator gets a generated {@code CHECK} constraint that rejects the concrete
 * {@code Lecture}/{@code Quiz} discriminator values. That constraint is an artefact of Hibernate DDL
 * generation only - the production schema is managed by Liquibase (see {@code db/courses.yml}) which
 * adds the {@code version BIGINT} column with no such check. We therefore drop the generated check
 * constraint before each test so the {@code @Version} mechanics can be exercised on a persisted
 * {@code CurriculumItem}, closing the behavioural gap that the metamodel assertion in
 * {@link CourseOptimisticLockingTest} could only approximate. Writes go through {@link CourseRepository}
 * (the production path - the item is cascade-persisted from its {@code Course} aggregate root) so the
 * conflict surfaces as the Spring {@link ObjectOptimisticLockingFailureException} the command handlers
 * retry on, rather than the raw JPA {@code OptimisticLockException}.
 */
@DataJpaTest
class CurriculumItemOptimisticLockingTest {

    private static final String TEACHER = "teacher";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @BeforeEach
    void dropGeneratedDiscriminatorCheckConstraint() {
        final EntityManager em = entityManager.getEntityManager();
        @SuppressWarnings("unchecked") final List<String> constraintNames = em.createNativeQuery(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_name = 'CURRICULUM_ITEM' AND constraint_type = 'CHECK'").getResultList();
        for (String name : constraintNames) {
            em.createNativeQuery("ALTER TABLE curriculum_item DROP CONSTRAINT \"" + name + "\"").executeUpdate();
        }
    }

    @Test
    void save_newCurriculumItem_versionInitializedToZero() {
        // given
        final UUID courseUuid = persistCourseWithLecture();
        entityManager.clear();

        // when
        final Course course = courseRepository.findByUuid(courseUuid).orElseThrow();
        final CurriculumItem item = singleCurriculumItem(course);

        // then
        assertThat(ReflectionTestUtils.getField(item, "version")).isEqualTo(0);
    }

    @Test
    void update_persistedCurriculumItem_versionIncremented() {
        // given - a persisted curriculum item at version 0
        final UUID courseUuid = persistCourseWithLecture();
        entityManager.clear();

        // when - a dirty update is saved through the aggregate root
        final Course course = courseRepository.findByUuid(courseUuid).orElseThrow();
        final CurriculumItem item = singleCurriculumItem(course);
        ReflectionTestUtils.setField(item, "title", "renamed");
        courseRepository.saveAndFlush(course);

        // then
        assertThat(ReflectionTestUtils.getField(item, "version")).isEqualTo(1);
    }

    @Test
    void saveAndFlush_staleCurriculumItem_throwsObjectOptimisticLockingFailureException() {
        // given - a persisted course whose single curriculum item is at version 0
        final UUID courseUuid = persistCourseWithLecture();
        entityManager.clear();

        // and - the aggregate loaded through its repository while the item version is still 0
        final Course course = courseRepository.findByUuid(courseUuid).orElseThrow();
        final CurriculumItem item = singleCurriculumItem(course);
        final Integer itemId = (Integer) ReflectionTestUtils.getField(item, "id");

        // and - a concurrent writer bumps the persisted curriculum_item version to 1 out-of-band
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE curriculum_item SET version = version + 1 WHERE id = :id")
                .setParameter("id", itemId)
                .executeUpdate();

        // when - persisting a mutation made against the now-stale version-0 snapshot
        ReflectionTestUtils.setField(item, "title", "stale");

        // then - the cascaded versioned UPDATE matches no row and Spring surfaces the conflict
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> courseRepository.saveAndFlush(course));
    }

    private UUID persistCourseWithLecture() {
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand(TEACHER)));
        final CreateCurriculumItemCommand lecture = CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(lecture))
                .build();
        final Course course = new Course(command, teacher.getId());
        courseRepository.saveAndFlush(course);
        return course.toIdentity();
    }

    @SuppressWarnings("unchecked")
    private static CurriculumItem singleCurriculumItem(Course course) {
        final List<CurriculumItem> items = (List<CurriculumItem>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(1);
        return items.get(0);
    }
}
