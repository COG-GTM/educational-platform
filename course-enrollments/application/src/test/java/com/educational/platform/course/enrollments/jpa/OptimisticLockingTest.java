package com.educational.platform.course.enrollments.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

/**
 * Verifies the JPA optimistic locking (@Version) behaviour added to the
 * course-enrollments aggregates. The Hibernate-managed version field must be
 * initialised on insert, incremented on update and must make concurrent
 * updates of a stale aggregate fail fast.
 */
@DataJpaTest
public class OptimisticLockingTest {

	private static final int COURSE_ID = 1;
	private static final int STUDENT_ID = 1;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private CourseEnrollmentRepository courseEnrollmentRepository;

	@Autowired
	private EnrollCourseRepository enrollCourseRepository;

	@Autowired
	private StudentRepository studentRepository;

	// --- CourseEnrollment ---------------------------------------------------

	@Test
	void persist_newCourseEnrollment_versionInitializedToZero() {
		// given
		final CourseEnrollment enrollment = new CourseEnrollment(COURSE_ID, STUDENT_ID);
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", null);

		// when
		final CourseEnrollment saved = entityManager.persistFlushFind(enrollment);

		// then
		assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
	}

	@Test
	void update_modifiedCourseEnrollment_versionIncremented() {
		// given
		final CourseEnrollment enrollment = entityManager
				.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID));
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", 0);

		// when
		enrollment.complete();
		entityManager.flush();

		// then
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void save_staleCourseEnrollment_concurrentlyUpdated_optimisticLockingFailure() {
		// given a persisted enrollment and a stale snapshot of it
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID)), Integer.class);
		entityManager.clear();
		final CourseEnrollment stale = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when another transaction updates the same row (version 0 -> 1)
		incrementVersion("course_enrollment", id);

		// then persisting the stale snapshot fails fast
		stale.complete();
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseEnrollmentRepository.saveAndFlush(stale));
	}

	// --- Student ------------------------------------------------------------

	@Test
	void persist_newStudent_versionInitializedToZero() {
		// given
		final Student student = new Student(new CreateStudentCommand("username"));
		assertThat(student).hasFieldOrPropertyWithValue("version", null);

		// when
		final Student saved = entityManager.persistFlushFind(student);

		// then
		assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
	}

	@Test
	void update_modifiedStudent_versionIncremented() {
		// given
		final Student student = entityManager.persistFlushFind(new Student(new CreateStudentCommand("username")));
		assertThat(student).hasFieldOrPropertyWithValue("version", 0);

		// when (no domain setter exists, so dirty the managed field directly)
		ReflectionTestUtils.setField(student, "username", "renamed");
		entityManager.flush();

		// then
		assertThat(student).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void save_staleStudent_concurrentlyUpdated_optimisticLockingFailure() {
		// given a persisted student and a stale snapshot of it
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new Student(new CreateStudentCommand("username"))), Integer.class);
		entityManager.clear();
		final Student stale = studentRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when another transaction updates the same row (version 0 -> 1)
		incrementVersion("student", id);

		// then persisting the stale snapshot fails fast
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> studentRepository.saveAndFlush(stale));
	}

	// --- EnrollCourse -------------------------------------------------------

	@Test
	void persist_newEnrollCourse_versionInitializedToZero() {
		// given
		final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()));
		assertThat(course).hasFieldOrPropertyWithValue("version", null);

		// when
		final EnrollCourse saved = entityManager.persistFlushFind(course);

		// then
		assertThat(saved).hasFieldOrPropertyWithValue("version", 0);
	}

	@Test
	void update_modifiedEnrollCourse_versionIncremented() {
		// given
		final EnrollCourse course = entityManager
				.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID())));
		assertThat(course).hasFieldOrPropertyWithValue("version", 0);

		// when (no domain setter exists, so dirty the managed field directly)
		ReflectionTestUtils.setField(course, "uuid", UUID.randomUUID());
		entityManager.flush();

		// then
		assertThat(course).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void save_staleEnrollCourse_concurrentlyUpdated_optimisticLockingFailure() {
		// given a persisted course and a stale snapshot of it
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()))),
						Integer.class);
		entityManager.clear();
		final EnrollCourse stale = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when another transaction updates the same row (version 0 -> 1)
		incrementVersion("enroll_course", id);

		// then persisting the stale snapshot fails fast
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> enrollCourseRepository.saveAndFlush(stale));
	}

	/**
	 * Simulates a concurrent transaction by bumping the version column of the
	 * given row directly in the database, bypassing the persistence context.
	 */
	private void incrementVersion(String table, Integer id) {
		entityManager.getEntityManager()
				.createNativeQuery("update " + table + " set version = version + 1 where id = :id")
				.setParameter("id", id)
				.executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

}
