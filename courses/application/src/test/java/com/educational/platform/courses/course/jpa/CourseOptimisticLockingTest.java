package com.educational.platform.courses.course.jpa;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurriculumItem;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies the JPA optimistic locking ({@code @Version}) behaviour added to the courses entities.
 * This is the persistence-level contract the retrying command handlers rely on.
 */
@DataJpaTest
class CourseOptimisticLockingTest {

    private static final String TEACHER = "teacher";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void save_newCourse_versionInitializedToZero() {
        // given
        final Course course = newCourse(persistTeacher().getId());

        // when
        courseRepository.saveAndFlush(course);

        // then
        assertThat(ReflectionTestUtils.getField(course, "version")).isEqualTo(0);
    }

    @Test
    void save_newTeacher_versionInitializedToZero() {
        // given
        final Teacher teacher = new Teacher(new CreateTeacherCommand(TEACHER));

        // when
        teacherRepository.saveAndFlush(teacher);

        // then
        assertThat(ReflectionTestUtils.getField(teacher, "version")).isEqualTo(0);
    }

    @Test
    void update_persistedCourse_versionIncremented() {
        // given
        final Course course = newCourse(persistTeacher().getId());
        final UUID uuid = course.toIdentity();
        courseRepository.saveAndFlush(course);
        entityManager.clear();

        // when
        final Course loaded = courseRepository.findByUuid(uuid).orElseThrow();
        loaded.increaseNumberOfStudents();
        courseRepository.saveAndFlush(loaded);

        // then
        assertThat(ReflectionTestUtils.getField(loaded, "version")).isEqualTo(1);
    }

    @Test
    void update_persistedCourseViaRatingUpdate_versionIncremented() {
        // given - a persisted course at version 0
        final Course course = newCourse(persistTeacher().getId());
        final UUID uuid = course.toIdentity();
        courseRepository.saveAndFlush(course);
        entityManager.clear();

        // when - the rating mutator is flushed. The other version tests drive the optimistic-lock
        // column through increaseNumberOfStudents() only; this pins that updateRating() - the second
        // @Retryable handler's mutation - likewise issues a versioned UPDATE, the persistence-level
        // contract UpdateCourseRatingCommandHandler's retry relies on
        final Course loaded = courseRepository.findByUuid(uuid).orElseThrow();
        loaded.updateRating(4.5);
        courseRepository.saveAndFlush(loaded);

        // then
        assertThat(ReflectionTestUtils.getField(loaded, "version")).isEqualTo(1);
        assertThat(loaded).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void update_persistedCourseRepeatedly_versionIncrementsOncePerFlush() {
        // given - a persisted course at version 0
        final Course course = newCourse(persistTeacher().getId());
        final UUID uuid = course.toIdentity();
        courseRepository.saveAndFlush(course);

        // when - three independent reload -> mutate -> flush cycles (one per "retry attempt")
        for (int i = 0; i < 3; i++) {
            entityManager.clear();
            final Course loaded = courseRepository.findByUuid(uuid).orElseThrow();
            loaded.increaseNumberOfStudents();
            courseRepository.saveAndFlush(loaded);
        }

        // then - the version advances exactly once per flush and the mutations accumulate
        entityManager.clear();
        final Course reloaded = courseRepository.findByUuid(uuid).orElseThrow();
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(3);
        assertThat(reloaded).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }

    @Test
    void update_multipleMutationsBeforeSingleFlush_versionIncrementsOnce() {
        // given - a persisted course at version 0
        final Course course = newCourse(persistTeacher().getId());
        final UUID uuid = course.toIdentity();
        courseRepository.saveAndFlush(course);
        entityManager.clear();

        // when - two mutations are applied to the same managed instance before a single flush
        final Course loaded = courseRepository.findByUuid(uuid).orElseThrow();
        loaded.increaseNumberOfStudents();
        loaded.increaseNumberOfStudents();
        courseRepository.saveAndFlush(loaded);

        // then - the optimistic-lock version tracks flushed UPDATEs, not in-memory mutator calls, so it
        // advances exactly once despite two mutations; the state changes still accumulate to two. This
        // is the invariant that lets each retry attempt (one fresh transaction = one flush) bump the
        // version by exactly one, distinct from the once-per-flush test that flushes three times
        assertThat(ReflectionTestUtils.getField(loaded, "version")).isEqualTo(1);
        assertThat(loaded).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void saveAndFlush_reloadAfterConcurrentUpdate_succeedsWithIncrementedVersion() {
        // given - a persisted course at version 0
        final Course course = newCourse(persistTeacher().getId());
        final UUID uuid = course.toIdentity();
        courseRepository.saveAndFlush(course);
        entityManager.clear();

        // and - a concurrent writer bumps the persisted version to 1
        final Course concurrent = courseRepository.findByUuid(uuid).orElseThrow();
        concurrent.increaseNumberOfStudents();
        courseRepository.saveAndFlush(concurrent);
        entityManager.clear();

        // when - reloading a fresh copy (what a retry attempt does) and saving it
        final Course reloaded = courseRepository.findByUuid(uuid).orElseThrow();
        reloaded.increaseNumberOfStudents();
        courseRepository.saveAndFlush(reloaded);

        // then - the reload sees version 1 and the save succeeds, advancing it to 2
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
        assertThat(reloaded).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void saveAndFlush_staleCourse_throwsObjectOptimisticLockingFailureException() {
        // given - a persisted course at version 0
        final Course course = newCourse(persistTeacher().getId());
        final UUID uuid = course.toIdentity();
        courseRepository.saveAndFlush(course);
        entityManager.clear();

        // and - a detached copy snapshotting version 0
        final Course stale = courseRepository.findByUuid(uuid).orElseThrow();
        entityManager.detach(stale);

        // and - a concurrent update bumps the persisted version to 1
        final Course concurrent = courseRepository.findByUuid(uuid).orElseThrow();
        concurrent.increaseNumberOfStudents();
        courseRepository.saveAndFlush(concurrent);
        entityManager.clear();

        // when - persisting the now-stale copy
        stale.increaseNumberOfStudents();

        // then
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> courseRepository.saveAndFlush(stale));
    }

    @Test
    void saveAndFlush_staleTeacher_throwsObjectOptimisticLockingFailureException() {
        // given - a persisted teacher at version 0
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand(TEACHER)));
        final Integer id = teacher.getId();
        entityManager.clear();

        // and - a detached copy snapshotting version 0
        final Teacher stale = teacherRepository.findById(id).orElseThrow();
        entityManager.detach(stale);

        // and - a concurrent update bumps the persisted version to 1
        final Teacher concurrent = teacherRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(concurrent, "username", "concurrent");
        teacherRepository.saveAndFlush(concurrent);
        entityManager.clear();

        // when - persisting the now-stale copy
        ReflectionTestUtils.setField(stale, "username", "stale");

        // then
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> teacherRepository.saveAndFlush(stale));
    }

    @Test
    void update_persistedTeacher_versionIncremented() {
        // given
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand(TEACHER)));
        final Integer id = teacher.getId();
        entityManager.clear();

        // when - a dirty update is flushed
        final Teacher loaded = teacherRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(loaded, "username", "renamed");
        teacherRepository.saveAndFlush(loaded);

        // then
        assertThat(ReflectionTestUtils.getField(loaded, "version")).isEqualTo(1);
    }

    @Test
    void curriculumItem_isMappedWithOptimisticLockVersion() {
        // asserts the persistence provider recognises an optimistic-lock version attribute on the entity
        final boolean hasVersion = entityManager.getEntityManager()
                .getMetamodel()
                .entity(CurriculumItem.class)
                .hasVersionAttribute();

        assertThat(hasVersion).isTrue();
    }

    @Test
    void curriculumItem_versionAttributeIsNamedVersionAndTypedInteger() {
        // Pins the CurriculumItem mapping contract: the @Version field is the optimistic-lock
        // version, named "version" and typed Integer (matching the BIGINT column the Liquibase change
        // set adds to curriculum_item). The increment/conflict mechanics are exercised behaviourally
        // in CurriculumItemOptimisticLockingTest (which has to drop Hibernate's generated
        // single-table-inheritance discriminator check constraint to insert a concrete item).
        final var versionAttribute = entityManager.getEntityManager()
                .getMetamodel()
                .entity(CurriculumItem.class)
                .getVersion(Integer.class);

        assertThat(versionAttribute.isVersion()).isTrue();
        assertThat(versionAttribute.getName()).isEqualTo("version");
        assertThat(versionAttribute.getJavaType()).isEqualTo(Integer.class);
    }

    @Test
    void course_versionAttributeIsNamedVersionAndTypedInteger() {
        final var versionAttribute = entityManager.getEntityManager()
                .getMetamodel()
                .entity(Course.class)
                .getVersion(Integer.class);

        assertThat(versionAttribute.isVersion()).isTrue();
        assertThat(versionAttribute.getName()).isEqualTo("version");
        assertThat(versionAttribute.getJavaType()).isEqualTo(Integer.class);
    }

    @Test
    void teacher_versionAttributeIsNamedVersionAndTypedInteger() {
        final var versionAttribute = entityManager.getEntityManager()
                .getMetamodel()
                .entity(Teacher.class)
                .getVersion(Integer.class);

        assertThat(versionAttribute.isVersion()).isTrue();
        assertThat(versionAttribute.getName()).isEqualTo("version");
        assertThat(versionAttribute.getJavaType()).isEqualTo(Integer.class);
    }

    @Test
    void update_persistedTeacherRepeatedly_versionIncrementsOncePerFlush() {
        // given - a persisted teacher at version 0
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand(TEACHER)));
        final Integer id = teacher.getId();

        // when - three independent reload -> mutate -> flush cycles
        for (int i = 0; i < 3; i++) {
            entityManager.clear();
            final Teacher loaded = teacherRepository.findById(id).orElseThrow();
            ReflectionTestUtils.setField(loaded, "username", "renamed-" + i);
            teacherRepository.saveAndFlush(loaded);
        }

        // then - the version advances exactly once per flush
        entityManager.clear();
        final Teacher reloaded = teacherRepository.findById(id).orElseThrow();
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(3);
    }

    @Test
    void saveAndFlush_reloadTeacherAfterConcurrentUpdate_succeedsWithIncrementedVersion() {
        // given - a persisted teacher at version 0
        final Teacher teacher = teacherRepository.saveAndFlush(new Teacher(new CreateTeacherCommand(TEACHER)));
        final Integer id = teacher.getId();
        entityManager.clear();

        // and - a concurrent writer bumps the persisted version to 1
        final Teacher concurrent = teacherRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(concurrent, "username", "concurrent");
        teacherRepository.saveAndFlush(concurrent);
        entityManager.clear();

        // when - reloading a fresh copy (what a retry attempt does) and saving it
        final Teacher reloaded = teacherRepository.findById(id).orElseThrow();
        ReflectionTestUtils.setField(reloaded, "username", "retried");
        teacherRepository.saveAndFlush(reloaded);

        // then - the reload sees version 1 and the save succeeds, advancing it to 2
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
        assertThat(reloaded.toIdentity()).isEqualTo("retried");
    }

    private Teacher persistTeacher() {
        final Teacher teacher = new Teacher(new CreateTeacherCommand(TEACHER));
        return teacherRepository.saveAndFlush(teacher);
    }

    private static Course newCourse(Integer teacherId) {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, teacherId);
    }
}
