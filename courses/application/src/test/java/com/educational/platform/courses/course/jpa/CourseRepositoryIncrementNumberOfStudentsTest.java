package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

@DataJpaTest
public class CourseRepositoryIncrementNumberOfStudentsTest {

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TestEntityManager entityManager;

	private Teacher teacher;

	@BeforeEach
	void setUp() {
		teacher = new Teacher(new CreateTeacherCommand("teacher"));
		teacherRepository.save(teacher);
	}

	@Test
	void incrementNumberOfStudents_existingCourse_numberIncrementedAndOneRowUpdated() {
		// given
		final Course course = course("name");

		// when
		final int updatedRows = courseRepository.incrementNumberOfStudents(course.toIdentity());

		// then
		assertThat(updatedRows).isEqualTo(1);
		entityManager.clear();
		final Course result = courseRepository.findByUuid(course.toIdentity()).orElseThrow();
		assertThat(result).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
	}

	@Test
	void incrementNumberOfStudents_calledTwice_numberIncrementedTwice() {
		// given
		final Course course = course("name");

		// when
		courseRepository.incrementNumberOfStudents(course.toIdentity());
		courseRepository.incrementNumberOfStudents(course.toIdentity());

		// then
		entityManager.clear();
		final Course result = courseRepository.findByUuid(course.toIdentity()).orElseThrow();
		assertThat(result).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
	}

	@Test
	void incrementNumberOfStudents_nonExistingUuid_zeroRowsUpdated() {
		// given
		final UUID unknownUuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440999");

		// when
		final int updatedRows = courseRepository.incrementNumberOfStudents(unknownUuid);

		// then
		assertThat(updatedRows).isZero();
	}

	@Test
	void incrementNumberOfStudents_existingCourse_otherCoursesUnaffected() {
		// given
		final Course course = course("name");
		final Course other = course("other");

		// when
		courseRepository.incrementNumberOfStudents(course.toIdentity());

		// then
		entityManager.clear();
		final Course untouched = courseRepository.findByUuid(other.toIdentity()).orElseThrow();
		assertThat(untouched).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
	}

	private Course course(String name) {
		final CreateCourseCommand command = CreateCourseCommand.builder().name(name).description("description").build();
		final Course course = new Course(command, teacher.getId());
		courseRepository.save(course);
		entityManager.flush();
		return course;
	}

}
