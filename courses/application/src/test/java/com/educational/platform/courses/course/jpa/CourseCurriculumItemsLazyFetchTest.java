package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
public class CourseCurriculumItemsLazyFetchTest {

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findByUuid_persistedCourse_curriculumItemsNotFetchedEagerly() {
		// given
		var teacher = new Teacher(new CreateTeacherCommand("teacher"));
		teacherRepository.save(teacher);
		var command = CreateCourseCommand.builder().name("name").description("description").build();
		var course = new Course(command, teacher.getId());
		courseRepository.save(course);
		entityManager.flush();
		entityManager.clear();

		// when
		var result = courseRepository.findByUuid(course.toIdentity()).orElseThrow();

		// then
		var persistenceUnitUtil = entityManager.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
		assertThat(persistenceUnitUtil.isLoaded(result, "curriculumItems")).isFalse();
	}
}
