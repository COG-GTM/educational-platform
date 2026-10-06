package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
	void findPublishedDetails_publishedCourse_detailsRetrieved() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var course = new Course(CreateCourseCommand.builder().name("name").description("description").category("Programming").build(), teacher.getId());
		course.approve();
		course.publish();
		course.updateRating(4.5);
		course.increaseNumberOfStudents();
		courseRepository.save(course);

		// when
		var result = courseRepository.findPublishedDetails(course.toIdentity());

		// then
		assertThat(result).isPresent();
		assertThat(result.get())
				.hasFieldOrPropertyWithValue("uuid", course.toIdentity())
				.hasFieldOrPropertyWithValue("name", "name")
				.hasFieldOrPropertyWithValue("description", "description")
				.hasFieldOrPropertyWithValue("category", "Programming")
				.hasFieldOrPropertyWithValue("teacherName", TEACHER)
				.hasFieldOrPropertyWithValue("rating", 4.5)
				.hasFieldOrPropertyWithValue("numberOfStudents", 1);
		assertThat(result.get().publishedDate()).isNotNull();
		assertThat(result.get().curriculumItems()).isEmpty();
	}

	@Test
	void findPublishedDetails_draftCourse_empty() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var course = courseRepository.save(new Course(CreateCourseCommand.builder().name("name").description("description").build(), teacher.getId()));

		// when
		var result = courseRepository.findPublishedDetails(course.toIdentity());

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void findPublishedDetails_archivedCourse_empty() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), teacher.getId());
		course.approve();
		course.publish();
		course.archive();
		courseRepository.save(course);

		// when
		var result = courseRepository.findPublishedDetails(course.toIdentity());

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void findPublishedDetails_unknownCourse_empty() {
		// given/when
		var result = courseRepository.findPublishedDetails(UUID.randomUUID());

		// then
		assertThat(result).isEmpty();
	}

}
