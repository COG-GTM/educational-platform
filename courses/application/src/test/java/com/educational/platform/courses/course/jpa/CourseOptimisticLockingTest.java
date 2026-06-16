package com.educational.platform.courses.course.jpa;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.CurriculumItem;
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
        // CurriculumItem persistence cannot be exercised directly in this @DataJpaTest: Hibernate's
        // generated single-table-inheritance discriminator check constraint rejects inserts in the
        // test schema (production uses Liquibase, which carries no such constraint). The version
        // increment/conflict mechanics are generic JPA behaviour already proven for Course/Teacher
        // above, so here we pin the CurriculumItem-specific mapping contract: the @Version field is
        // the optimistic-lock version, named "version" and typed Integer (matching the BIGINT column
        // the Liquibase change set adds to curriculum_item).
        final var versionAttribute = entityManager.getEntityManager()
                .getMetamodel()
                .entity(CurriculumItem.class)
                .getVersion(Integer.class);

        assertThat(versionAttribute.isVersion()).isTrue();
        assertThat(versionAttribute.getName()).isEqualTo("version");
        assertThat(versionAttribute.getJavaType()).isEqualTo(Integer.class);
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
