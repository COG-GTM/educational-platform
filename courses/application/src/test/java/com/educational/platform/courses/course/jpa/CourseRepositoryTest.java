package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
public class CourseRepositoryTest {

	private static final String TEACHER = "teacher";

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void increaseNumberOfStudents_savedCourse_incrementedCountPersistedAndReturnedInDtos() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var command = CreateCourseCommand.builder().name("name").description("description").build();
		var course = courseRepository.saveAndFlush(new Course(command, teacher.getId()));
		var uuid = course.toIdentity();
		entityManager.clear();
		var sut = courseRepository.findByUuid(uuid).orElseThrow();

		// when
		sut.increaseNumberOfStudents();
		courseRepository.saveAndFlush(sut);
		entityManager.clear();

		// then
		assertThat(courseRepository.findByUuid(uuid)).get()
				.hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
		assertThat(courseRepository.list()).singleElement()
				.hasFieldOrPropertyWithValue("uuid", uuid)
				.hasFieldOrPropertyWithValue("numberOfStudents", 1);
		assertThat(courseRepository.findDTOByUuid(uuid)).get()
				.hasFieldOrPropertyWithValue("numberOfStudents", 1);
	}

	@Test
	void queryDtoByUUID_validQuery_dtoRetrieved() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		var createCourseCommand = CreateCourseCommand.builder().name("name").description("description").build();
		var course = new Course(createCourseCommand, teacher.getId());
		courseRepository.save(course);

		// when
		var result = courseRepository.findDTOByUuid(course.toIdentity());

		// then
		assertThat(result).isNotEmpty();
		assertThat(result.get()).hasFieldOrPropertyWithValue("name", "name").hasFieldOrPropertyWithValue("description", "description");
	}

}
