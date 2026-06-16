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

import com.educational.platform.course.enrollments.CompletionStatus;
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
	void flush_unchangedCourseEnrollment_versionNotIncremented() {
		// given a persisted enrollment at the initial version
		final CourseEnrollment enrollment = entityManager
				.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID));
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", 0);

		// when it is flushed again without any state change
		entityManager.flush();

		// then the version is left untouched (only real updates bump it)
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", 0);
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

	@Test
	void saveAndFlush_concurrentlyEditedCourseEnrollment_secondWriterFailsFast() {
		// given two independent detached snapshots of the same persisted enrollment
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID)), Integer.class);
		entityManager.clear();
		final CourseEnrollment firstWriter = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(firstWriter);
		entityManager.clear();
		final CourseEnrollment secondWriter = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(secondWriter);

		// when the first writer commits its change through the repository (version 0 -> 1)
		firstWriter.complete();
		courseEnrollmentRepository.saveAndFlush(firstWriter);
		entityManager.clear();

		// then the second writer, still holding version 0, cannot silently overwrite it
		secondWriter.complete();
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseEnrollmentRepository.saveAndFlush(secondWriter));
	}

	@Test
	void saveAndFlush_concurrentlyEditedCourseEnrollment_winningChangeSurvivesAndStaleWriteIsRejected() {
		// given a persisted enrollment and a stale snapshot taken before any change
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID)), Integer.class);
		entityManager.clear();
		final CourseEnrollment stale = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when a concurrent writer completes the enrollment through the repository (version 0 -> 1)
		final CourseEnrollment winner = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(winner);
		winner.complete();
		courseEnrollmentRepository.saveAndFlush(winner);
		entityManager.clear();

		// then the winning change is durably persisted
		final CourseEnrollment persisted = courseEnrollmentRepository.findById(id).orElseThrow();
		assertThat(persisted).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.COMPLETED);
		assertThat(persisted).hasFieldOrPropertyWithValue("student", STUDENT_ID);
		assertThat(persisted).hasFieldOrPropertyWithValue("version", 1);
		entityManager.clear();

		// and the stale writer cannot silently overwrite it with a competing edit
		ReflectionTestUtils.setField(stale, "student", STUDENT_ID + 1);
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> courseEnrollmentRepository.saveAndFlush(stale));
	}

	@Test
	void update_courseEnrollmentMultipleTimes_versionIncrementsMonotonically() {
		// given
		final CourseEnrollment enrollment = entityManager
				.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID));
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", 0);

		// when the aggregate is updated twice in separate flushes
		enrollment.complete();
		entityManager.flush();
		ReflectionTestUtils.setField(enrollment, "student", STUDENT_ID + 1);
		entityManager.flush();

		// then the version is bumped once per update, never skipping or resetting
		assertThat(enrollment).hasFieldOrPropertyWithValue("version", 2);
	}

	@Test
	void save_currentCourseEnrollment_notConcurrentlyUpdated_succeedsAndIncrementsVersion() {
		// given a persisted enrollment reloaded as a detached, up-to-date snapshot
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID)), Integer.class);
		entityManager.clear();
		final CourseEnrollment current = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(current);

		// when it is updated without any competing change to the row
		current.complete();
		final CourseEnrollment saved = courseEnrollmentRepository.saveAndFlush(current);

		// then the optimistic lock does not fire and the version advances
		assertThat(saved).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void find_updatedCourseEnrollment_versionRoundTripsThroughBigintColumn() {
		// given a persisted enrollment whose version has been bumped to 1
		final CourseEnrollment enrollment = entityManager
				.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID));
		final Integer id = entityManager.getId(enrollment, Integer.class);
		enrollment.complete();
		entityManager.flush();

		// when the row is read back from the BIGINT version column
		entityManager.clear();
		final CourseEnrollment reloaded = courseEnrollmentRepository.findById(id).orElseThrow();

		// then the non-zero version is mapped back onto the Integer field
		assertThat(reloaded).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void delete_staleCourseEnrollment_concurrentlyUpdated_optimisticLockingFailure() {
		// given a persisted enrollment and a stale snapshot of it
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new CourseEnrollment(COURSE_ID, STUDENT_ID)), Integer.class);
		entityManager.clear();
		final CourseEnrollment stale = courseEnrollmentRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when another transaction updates the same row (version 0 -> 1)
		incrementVersion("course_enrollment", id);

		// then deleting the stale snapshot fails fast instead of dropping fresh data
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					courseEnrollmentRepository.delete(stale);
					courseEnrollmentRepository.flush();
				});
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
	void flush_unchangedStudent_versionNotIncremented() {
		// given a persisted student at the initial version
		final Student student = entityManager.persistFlushFind(new Student(new CreateStudentCommand("username")));
		assertThat(student).hasFieldOrPropertyWithValue("version", 0);

		// when it is flushed again without any state change
		entityManager.flush();

		// then the version is left untouched (only real updates bump it)
		assertThat(student).hasFieldOrPropertyWithValue("version", 0);
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

	@Test
	void saveAndFlush_concurrentlyEditedStudent_secondWriterFailsFast() {
		// given two independent detached snapshots of the same persisted student
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new Student(new CreateStudentCommand("username"))), Integer.class);
		entityManager.clear();
		final Student firstWriter = studentRepository.findById(id).orElseThrow();
		entityManager.detach(firstWriter);
		entityManager.clear();
		final Student secondWriter = studentRepository.findById(id).orElseThrow();
		entityManager.detach(secondWriter);

		// when the first writer commits its change through the repository (version 0 -> 1)
		ReflectionTestUtils.setField(firstWriter, "username", "renamed-by-first");
		studentRepository.saveAndFlush(firstWriter);
		entityManager.clear();

		// then the second writer, still holding version 0, cannot silently overwrite it
		ReflectionTestUtils.setField(secondWriter, "username", "renamed-by-second");
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> studentRepository.saveAndFlush(secondWriter));
	}

	@Test
	void saveAndFlush_concurrentlyEditedStudent_winningChangeSurvivesAndStaleWriteIsRejected() {
		// given a persisted student and a stale snapshot taken before any change
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new Student(new CreateStudentCommand("username"))), Integer.class);
		entityManager.clear();
		final Student stale = studentRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when a concurrent writer renames the student through the repository (version 0 -> 1)
		final Student winner = studentRepository.findById(id).orElseThrow();
		entityManager.detach(winner);
		ReflectionTestUtils.setField(winner, "username", "renamed-by-winner");
		studentRepository.saveAndFlush(winner);
		entityManager.clear();

		// then the winning change is durably persisted
		final Student persisted = studentRepository.findById(id).orElseThrow();
		assertThat(persisted).hasFieldOrPropertyWithValue("username", "renamed-by-winner");
		assertThat(persisted).hasFieldOrPropertyWithValue("version", 1);
		entityManager.clear();

		// and the stale writer cannot silently overwrite it with a competing edit
		ReflectionTestUtils.setField(stale, "username", "renamed-by-loser");
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> studentRepository.saveAndFlush(stale));
	}

	@Test
	void update_studentMultipleTimes_versionIncrementsMonotonically() {
		// given
		final Student student = entityManager.persistFlushFind(new Student(new CreateStudentCommand("username")));
		assertThat(student).hasFieldOrPropertyWithValue("version", 0);

		// when the aggregate is updated twice in separate flushes
		ReflectionTestUtils.setField(student, "username", "renamed");
		entityManager.flush();
		ReflectionTestUtils.setField(student, "username", "renamed-again");
		entityManager.flush();

		// then the version is bumped once per update, never skipping or resetting
		assertThat(student).hasFieldOrPropertyWithValue("version", 2);
	}

	@Test
	void save_currentStudent_notConcurrentlyUpdated_succeedsAndIncrementsVersion() {
		// given a persisted student reloaded as a detached, up-to-date snapshot
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new Student(new CreateStudentCommand("username"))), Integer.class);
		entityManager.clear();
		final Student current = studentRepository.findById(id).orElseThrow();
		entityManager.detach(current);

		// when it is updated without any competing change to the row
		ReflectionTestUtils.setField(current, "username", "renamed");
		final Student saved = studentRepository.saveAndFlush(current);

		// then the optimistic lock does not fire and the version advances
		assertThat(saved).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void find_updatedStudent_versionRoundTripsThroughBigintColumn() {
		// given a persisted student whose version has been bumped to 1
		final Student student = entityManager.persistFlushFind(new Student(new CreateStudentCommand("username")));
		final Integer id = entityManager.getId(student, Integer.class);
		ReflectionTestUtils.setField(student, "username", "renamed");
		entityManager.flush();

		// when the row is read back from the BIGINT version column
		entityManager.clear();
		final Student reloaded = studentRepository.findById(id).orElseThrow();

		// then the non-zero version is mapped back onto the Integer field
		assertThat(reloaded).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void delete_staleStudent_concurrentlyUpdated_optimisticLockingFailure() {
		// given a persisted student and a stale snapshot of it
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new Student(new CreateStudentCommand("username"))), Integer.class);
		entityManager.clear();
		final Student stale = studentRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when another transaction updates the same row (version 0 -> 1)
		incrementVersion("student", id);

		// then deleting the stale snapshot fails fast instead of dropping fresh data
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					studentRepository.delete(stale);
					studentRepository.flush();
				});
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
	void flush_unchangedEnrollCourse_versionNotIncremented() {
		// given a persisted course at the initial version
		final EnrollCourse course = entityManager
				.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID())));
		assertThat(course).hasFieldOrPropertyWithValue("version", 0);

		// when it is flushed again without any state change
		entityManager.flush();

		// then the version is left untouched (only real updates bump it)
		assertThat(course).hasFieldOrPropertyWithValue("version", 0);
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

	@Test
	void saveAndFlush_concurrentlyEditedEnrollCourse_secondWriterFailsFast() {
		// given two independent detached snapshots of the same persisted course
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()))),
						Integer.class);
		entityManager.clear();
		final EnrollCourse firstWriter = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(firstWriter);
		entityManager.clear();
		final EnrollCourse secondWriter = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(secondWriter);

		// when the first writer commits its change through the repository (version 0 -> 1)
		ReflectionTestUtils.setField(firstWriter, "uuid", UUID.randomUUID());
		enrollCourseRepository.saveAndFlush(firstWriter);
		entityManager.clear();

		// then the second writer, still holding version 0, cannot silently overwrite it
		ReflectionTestUtils.setField(secondWriter, "uuid", UUID.randomUUID());
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> enrollCourseRepository.saveAndFlush(secondWriter));
	}

	@Test
	void saveAndFlush_concurrentlyEditedEnrollCourse_winningChangeSurvivesAndStaleWriteIsRejected() {
		// given a persisted course and a stale snapshot taken before any change
		final UUID winningUuid = UUID.randomUUID();
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()))),
						Integer.class);
		entityManager.clear();
		final EnrollCourse stale = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when a concurrent writer changes the course through the repository (version 0 -> 1)
		final EnrollCourse winner = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(winner);
		ReflectionTestUtils.setField(winner, "uuid", winningUuid);
		enrollCourseRepository.saveAndFlush(winner);
		entityManager.clear();

		// then the winning change is durably persisted
		final EnrollCourse persisted = enrollCourseRepository.findById(id).orElseThrow();
		assertThat(persisted).hasFieldOrPropertyWithValue("uuid", winningUuid);
		assertThat(persisted).hasFieldOrPropertyWithValue("version", 1);
		entityManager.clear();

		// and the stale writer cannot silently overwrite it with a competing edit
		ReflectionTestUtils.setField(stale, "uuid", UUID.randomUUID());
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> enrollCourseRepository.saveAndFlush(stale));
	}

	@Test
	void update_enrollCourseMultipleTimes_versionIncrementsMonotonically() {
		// given
		final EnrollCourse course = entityManager
				.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID())));
		assertThat(course).hasFieldOrPropertyWithValue("version", 0);

		// when the aggregate is updated twice in separate flushes
		ReflectionTestUtils.setField(course, "uuid", UUID.randomUUID());
		entityManager.flush();
		ReflectionTestUtils.setField(course, "uuid", UUID.randomUUID());
		entityManager.flush();

		// then the version is bumped once per update, never skipping or resetting
		assertThat(course).hasFieldOrPropertyWithValue("version", 2);
	}

	@Test
	void save_currentEnrollCourse_notConcurrentlyUpdated_succeedsAndIncrementsVersion() {
		// given a persisted course reloaded as a detached, up-to-date snapshot
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()))),
						Integer.class);
		entityManager.clear();
		final EnrollCourse current = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(current);

		// when it is updated without any competing change to the row
		ReflectionTestUtils.setField(current, "uuid", UUID.randomUUID());
		final EnrollCourse saved = enrollCourseRepository.saveAndFlush(current);

		// then the optimistic lock does not fire and the version advances
		assertThat(saved).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void find_updatedEnrollCourse_versionRoundTripsThroughBigintColumn() {
		// given a persisted course whose version has been bumped to 1
		final EnrollCourse course = entityManager
				.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID())));
		final Integer id = entityManager.getId(course, Integer.class);
		ReflectionTestUtils.setField(course, "uuid", UUID.randomUUID());
		entityManager.flush();

		// when the row is read back from the BIGINT version column
		entityManager.clear();
		final EnrollCourse reloaded = enrollCourseRepository.findById(id).orElseThrow();

		// then the non-zero version is mapped back onto the Integer field
		assertThat(reloaded).hasFieldOrPropertyWithValue("version", 1);
	}

	@Test
	void delete_staleEnrollCourse_concurrentlyUpdated_optimisticLockingFailure() {
		// given a persisted course and a stale snapshot of it
		final Integer id = entityManager
				.getId(entityManager.persistFlushFind(new EnrollCourse(new CreateCourseCommand(UUID.randomUUID()))),
						Integer.class);
		entityManager.clear();
		final EnrollCourse stale = enrollCourseRepository.findById(id).orElseThrow();
		entityManager.detach(stale);

		// when another transaction updates the same row (version 0 -> 1)
		incrementVersion("enroll_course", id);

		// then deleting the stale snapshot fails fast instead of dropping fresh data
		assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
				.isThrownBy(() -> {
					enrollCourseRepository.delete(stale);
					enrollCourseRepository.flush();
				});
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
