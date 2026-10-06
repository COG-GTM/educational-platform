package com.educational.platform.course.enrollments.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.enrollments.CompletionStatus;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;

@Sql(scripts = "classpath:course.sql")
@DataJpaTest
public class CourseEnrollmentRepositoryTest {

	private static final String STUDENT = "student";
	private static final UUID FIRST_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
	private static final UUID FIRST_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440002");

	@Autowired
	private CourseEnrollmentRepository courseEnrollmentRepository;

	@Autowired
	private EnrollCourseRepository enrollCourseRepository;

	@Autowired
	private StudentRepository studentRepository;

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

	private Student student() {
		return studentRepository.findByUsername(STUDENT);
	}

	private CourseEnrollment createEnrollment(UUID courseUuid) {
		var course = enrollCourseRepository.findByUuid(courseUuid).orElseThrow();
		var enrollment = new CourseEnrollment(course, student());
		return courseEnrollmentRepository.save(enrollment);
	}

}
