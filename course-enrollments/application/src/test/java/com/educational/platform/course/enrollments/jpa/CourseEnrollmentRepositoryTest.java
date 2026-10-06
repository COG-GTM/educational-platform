package com.educational.platform.course.enrollments.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.enrollments.CompletionStatus;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

@Sql(scripts = "classpath:course.sql")
@DataJpaTest
public class CourseEnrollmentRepositoryTest {

	private static final String STUDENT = "student";
	private static final UUID FIRST_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
	private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");
	private static final UUID UNORDERED_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
	private static final UUID GENERICS_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440011");
	private static final UUID STREAMS_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440012");
	private static final Sort MOST_RECENT_ACTIVITY = Sort.by(Sort.Order.desc("lastActivityAt"), Sort.Order.desc("id"));

	@Autowired
	private CourseEnrollmentRepository courseEnrollmentRepository;

	@Autowired
	private EnrollCourseRepository enrollCourseRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findByStudent_createdCourseEnrollments_pageRetrieved() {
		// given
		createEnrollment(FIRST_COURSE);
		createEnrollment(SECOND_COURSE);

		// when
		var result = courseEnrollmentRepository.findByStudent(student(), PageRequest.of(0, 10));

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).extracting(e -> e.toDTO().course()).containsExactlyInAnyOrder(FIRST_COURSE, SECOND_COURSE);
		assertThat(result.getContent()).extracting(e -> e.toDTO().courseName()).containsExactlyInAnyOrder("Java Basics", "Spring Boot");
	}

	@Test
	void completedLectures_persisted_progressSurvivesReload() {
		// given
		var enrollment = createEnrollment(FIRST_COURSE);
		enrollment.completeLecture(FIRST_LECTURE);
		courseEnrollmentRepository.saveAndFlush(enrollment);

		// when
		var reloaded = courseEnrollmentRepository.findByUuidAndStudent(enrollment.getUuid(), student()).orElseThrow();

		// then
		var dto = reloaded.toDTO();
		assertThat(dto.completedLectures()).isEqualTo(1);
		assertThat(dto.totalLectures()).isEqualTo(2);
		assertThat(dto.progressPercent()).isEqualTo(50);
		assertThat(reloaded.toDetailsDTO().lectures()).extracting("uuid", "completed")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(FIRST_LECTURE, true), org.assertj.core.groups.Tuple.tuple(SECOND_LECTURE, false));
	}

	@Test
	void statusQueries_groupedByCompletionAndArchive() {
		// given
		var completed = createEnrollment(FIRST_COURSE);
		completed.completeLecture(FIRST_LECTURE);
		completed.completeLecture(SECOND_LECTURE);
		courseEnrollmentRepository.save(completed);
		var archived = createEnrollment(SECOND_COURSE);
		archived.archive();
		courseEnrollmentRepository.save(archived);

		// when / then
		assertThat(courseEnrollmentRepository.findByStudentAndArchivedFalseAndCompletionStatus(student(), CompletionStatus.COMPLETED, PageRequest.of(0, 10)).getContent())
				.extracting(CourseEnrollment::getUuid).containsExactly(completed.getUuid());
		assertThat(courseEnrollmentRepository.findByStudentAndArchivedFalseAndCompletionStatus(student(), CompletionStatus.IN_PROGRESS, PageRequest.of(0, 10)).getTotalElements())
				.isZero();
		assertThat(courseEnrollmentRepository.findByStudentAndArchivedTrue(student(), PageRequest.of(0, 10)).getContent())
				.extracting(CourseEnrollment::getUuid).containsExactly(archived.getUuid());
		assertThat(courseEnrollmentRepository.countByStudentAndArchivedFalseAndCompletionStatus(student(), CompletionStatus.COMPLETED)).isEqualTo(1);
		assertThat(courseEnrollmentRepository.countByStudentAndArchivedTrue(student())).isEqualTo(1);
	}

	@Test
	void findByCourseAndStudent_enrolled_found() {
		// given
		createEnrollment(FIRST_COURSE);

		// when / then
		assertThat(courseEnrollmentRepository.findFirstByCourseAndStudentOrderByIdAsc(enrollCourseRepository.findByUuid(FIRST_COURSE).orElseThrow(), student())).isPresent();
		assertThat(courseEnrollmentRepository.findFirstByCourseAndStudentOrderByIdAsc(enrollCourseRepository.findByUuid(SECOND_COURSE).orElseThrow(), student())).isEmpty();
	}

	@Test
	void findByStudent_sortedByLastActivity_recentlyActiveEnrollmentFirstDespiteLowerId() {
		// given
		var older = createEnrollment(FIRST_COURSE);
		var newer = createEnrollment(SECOND_COURSE);
		older.completeLecture(FIRST_LECTURE);
		courseEnrollmentRepository.saveAndFlush(older);

		// when
		var result = courseEnrollmentRepository.findByStudent(student(), PageRequest.of(0, 10, MOST_RECENT_ACTIVITY));

		// then
		assertThat(result.getContent()).extracting(CourseEnrollment::getUuid).containsExactly(older.getUuid(), newer.getUuid());
	}

	@Test
	void findByStudent_sortedByLastActivity_pagedAcrossPages() {
		// given
		var first = createEnrollment(FIRST_COURSE);
		var second = createEnrollment(SECOND_COURSE);
		var third = createEnrollment(UNORDERED_COURSE);

		// when
		var firstPage = courseEnrollmentRepository.findByStudent(student(), PageRequest.of(0, 2, MOST_RECENT_ACTIVITY));
		var secondPage = courseEnrollmentRepository.findByStudent(student(), PageRequest.of(1, 2, MOST_RECENT_ACTIVITY));

		// then
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
		assertThat(firstPage.getContent()).extracting(CourseEnrollment::getUuid).containsExactly(third.getUuid(), second.getUuid());
		assertThat(secondPage.getContent()).extracting(CourseEnrollment::getUuid).containsExactly(first.getUuid());
	}

	@Test
	void findByUuidAndStudent_enrollmentOfAnotherStudent_empty() {
		// given
		var enrollment = createEnrollment(FIRST_COURSE);
		var otherStudent = studentRepository.save(new Student(new CreateStudentCommand("other-student")));

		// when
		var result = courseEnrollmentRepository.findByUuidAndStudent(enrollment.getUuid(), otherStudent);

		// then
		assertThat(result).isEmpty();
		assertThat(courseEnrollmentRepository.findByUuidAndStudent(enrollment.getUuid(), student())).isPresent();
	}

	@Test
	void findFirstByCourseAndStudent_duplicateEnrollments_earliestReturned() {
		// given
		var earliest = createEnrollment(FIRST_COURSE);
		createEnrollment(FIRST_COURSE);
		var course = enrollCourseRepository.findByUuid(FIRST_COURSE).orElseThrow();

		// when
		var result = courseEnrollmentRepository.findFirstByCourseAndStudentOrderByIdAsc(course, student());

		// then
		assertThat(result).map(CourseEnrollment::getUuid).contains(earliest.getUuid());
	}

	@Test
	void findFirstByCourseAndStudent_enrollmentOfAnotherStudent_empty() {
		// given
		createEnrollment(FIRST_COURSE);
		var otherStudent = studentRepository.save(new Student(new CreateStudentCommand("other-student")));
		var course = enrollCourseRepository.findByUuid(FIRST_COURSE).orElseThrow();

		// when
		var result = courseEnrollmentRepository.findFirstByCourseAndStudentOrderByIdAsc(course, otherStudent);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void lectures_insertedOutOfOrder_loadedBySerialNumber() {
		// when
		var course = enrollCourseRepository.findByUuid(UNORDERED_COURSE).orElseThrow();
		var enrollment = createEnrollment(UNORDERED_COURSE);

		// then
		assertThat(course.getLectures()).extracting("uuid", "serialNumber")
				.containsExactly(org.assertj.core.groups.Tuple.tuple(GENERICS_LECTURE, 1), org.assertj.core.groups.Tuple.tuple(STREAMS_LECTURE, 2));
		assertThat(enrollment.toDetailsDTO().lectures()).extracting("title").containsExactly("Generics", "Streams");
	}

	@Test
	void refresh_persisted_droppedLectureRemovedKnownLectureKeepsIdentityAndProgressRecomputed() {
		// given
		var enrollment = createEnrollment(FIRST_COURSE);
		enrollment.completeLecture(SECOND_LECTURE);
		courseEnrollmentRepository.saveAndFlush(enrollment);
		var course = enrollCourseRepository.findByUuid(FIRST_COURSE).orElseThrow();
		var firstLectureId = course.lectureByUuid(FIRST_LECTURE).orElseThrow().getId();
		var newLecture = UUID.randomUUID();

		// when
		course.refresh(new CreateCourseCommand(FIRST_COURSE, "Java Basics (2nd edition)", List.of(
				new CreateCourseCommand.CreateLectureCommand(FIRST_LECTURE, "Introduction", 1),
				new CreateCourseCommand.CreateLectureCommand(newLecture, "Collections", 2))));
		enrollCourseRepository.saveAndFlush(course);
		entityManager.clear();

		// then
		var reloadedCourse = enrollCourseRepository.findByUuid(FIRST_COURSE).orElseThrow();
		assertThat(reloadedCourse.getName()).isEqualTo("Java Basics (2nd edition)");
		assertThat(reloadedCourse.getLectures()).extracting("uuid").containsExactly(FIRST_LECTURE, newLecture);
		assertThat(reloadedCourse.lectureByUuid(FIRST_LECTURE).orElseThrow().getId()).isEqualTo(firstLectureId);
		assertThat(reloadedCourse.lectureByUuid(FIRST_LECTURE).orElseThrow().getTitle()).isEqualTo("Introduction");
		assertThat(entityManager.getEntityManager()
				.createQuery("select count(l) from enroll_lecture l where l.uuid = :uuid", Long.class)
				.setParameter("uuid", SECOND_LECTURE).getSingleResult()).isZero();

		var reloadedEnrollment = courseEnrollmentRepository.findByUuidAndStudent(enrollment.getUuid(), student()).orElseThrow();
		var dto = reloadedEnrollment.toDTO();
		assertThat(dto.completedLectures()).isZero();
		assertThat(dto.totalLectures()).isEqualTo(2);
		assertThat(dto.progressPercent()).isZero();
		assertThat(dto.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
	}

	private Student student() {
		return studentRepository.findByUsername(STUDENT);
	}

	private CourseEnrollment createEnrollment(UUID courseUuid) {
		var course = enrollCourseRepository.findByUuid(courseUuid).orElseThrow();
		var enrollment = new CourseEnrollment(course, student());
		return courseEnrollmentRepository.save(enrollment);
	}

}
