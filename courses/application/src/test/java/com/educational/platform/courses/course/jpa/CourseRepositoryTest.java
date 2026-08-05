package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
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

	@Test
	void list_paged_pageOfCourses() {
		// given
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		for (int i = 0; i < 3; i++) {
			var createCourseCommand = CreateCourseCommand.builder().name("name" + i).description("description" + i).build();
			courseRepository.save(new Course(createCourseCommand, teacher.getId()));
		}

		// when
		var result = courseRepository.list(PageRequest.of(0, 2));

		// then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getTotalElements()).isEqualTo(3);
	}

}
