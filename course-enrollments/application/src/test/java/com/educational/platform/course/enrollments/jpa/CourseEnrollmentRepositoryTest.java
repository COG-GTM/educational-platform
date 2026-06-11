package com.educational.platform.course.enrollments.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.student.StudentRepository;

@Sql(scripts = "classpath:course.sql")
@DataJpaTest
public class CourseEnrollmentRepositoryTest {

	private static final String STUDENT = "student";
	private static final UUID FIRST_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
	private static final UUID SECOND_COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

	@Autowired
	private CourseEnrollmentRepository courseEnrollmentRepository;

	@Autowired
	private EnrollCourseRepository enrollCourseRepository;

	@Autowired
	private StudentRepository studentRepository;

	@Test
	void queryDtoByUUID_createdCourseEnrollment_dtoRetrieved() {
		// given
		createFirstCourseEnrollment();
		createSecondCourseEnrollment();

		// when
		var result = courseEnrollmentRepository.query(STUDENT);

		// then
		assertThat(result).isNotEmpty();
		assertThat(result).extracting("course").containsExactlyInAnyOrder(FIRST_COURSE, SECOND_COURSE);
	}

	@Test
	void findByUuid_existingEnrollment_returnsEnrollment() {
		// given
		var enrollment = createAndSaveEnrollment(FIRST_COURSE);

		// when
		var result = courseEnrollmentRepository.findByUuid(enrollment.getUuid());

		// then
		assertThat(result).isPresent();
		assertThat(result.get().getUuid()).isEqualTo(enrollment.getUuid());
	}

	@Test
	void findByUuid_nonExistingUuid_returnsEmpty() {
		// when
		var result = courseEnrollmentRepository.findByUuid(UUID.fromString("00000000-0000-0000-0000-000000000099"));

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void queryByUuidAndStudent_existingEnrollment_returnsDTO() {
		// given
		var enrollment = createAndSaveEnrollment(FIRST_COURSE);

		// when
		var result = courseEnrollmentRepository.query(enrollment.getUuid(), STUDENT);

		// then
		assertThat(result).isPresent();
		assertThat(result.get().uuid()).isEqualTo(enrollment.getUuid());
		assertThat(result.get().course()).isEqualTo(FIRST_COURSE);
		assertThat(result.get().student()).isEqualTo(STUDENT);
	}

	@Test
	void queryByUuidAndStudent_wrongStudent_returnsEmpty() {
		// given
		var enrollment = createAndSaveEnrollment(FIRST_COURSE);

		// when
		var result = courseEnrollmentRepository.query(enrollment.getUuid(), "unknown-student");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void queryByStudent_noEnrollments_returnsEmptyList() {
		// when
		var result = courseEnrollmentRepository.query("no-enrollments-student");

		// then
		assertThat(result).isEmpty();
	}

	private CourseEnrollment createAndSaveEnrollment(UUID courseUuid) {
		var course = enrollCourseRepository.findByUuid(courseUuid);
		var student = studentRepository.findByUsername(STUDENT);
		var enrollment = new CourseEnrollment(course.get().getId(), student.getId());
		return courseEnrollmentRepository.save(enrollment);
	}

	private void createFirstCourseEnrollment() {
		var course = enrollCourseRepository.findByUuid(FIRST_COURSE);
		var student = studentRepository.findByUsername(STUDENT);
		var first = new CourseEnrollment(course.get().getId(), student.getId());
		courseEnrollmentRepository.save(first);
	}

	private void createSecondCourseEnrollment() {
		var course = enrollCourseRepository.findByUuid(SECOND_COURSE);
		var student = studentRepository.findByUsername(STUDENT);
		var second = new CourseEnrollment(course.get().getId(), student.getId());
		courseEnrollmentRepository.save(second);
	}

}
