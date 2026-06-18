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
	void searchByKeyword_keywordMatchesName_courseRetrieved() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");
		givenCourse("Cooking 101", "Learn to bake bread");

		// when
		var result = courseRepository.searchByKeyword("Java");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).hasFieldOrPropertyWithValue("name", "Java Fundamentals")
				.hasFieldOrPropertyWithValue("description", "Introductory programming course");
	}

	@Test
	void searchByKeyword_keywordMatchesDescription_courseRetrieved() {
		// given
		givenCourse("Backend Bootcamp", "Build REST APIs with Spring Boot");
		givenCourse("Cooking 101", "Learn to bake bread");

		// when
		var result = courseRepository.searchByKeyword("Spring");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).hasFieldOrPropertyWithValue("name", "Backend Bootcamp");
	}

	@Test
	void searchByKeyword_partialKeyword_courseRetrieved() {
		// given
		givenCourse("Introduction to Programming", "description");

		// when
		var result = courseRepository.searchByKeyword("gramm");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).hasFieldOrPropertyWithValue("name", "Introduction to Programming");
	}

	@Test
	void searchByKeyword_noMatch_emptyList() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");

		// when
		var result = courseRepository.searchByKeyword("Python");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void searchByKeyword_sqlInjectionStyleKeyword_treatedAsLiteralAndReturnsNothing() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");

		// when
		var result = courseRepository.searchByKeyword("' OR '1'='1");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void searchByKeyword_keywordMatchesNameOfOneCourseAndDescriptionOfAnother_bothReturned() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");
		givenCourse("Backend Bootcamp", "Build REST APIs with Java");
		givenCourse("Cooking 101", "Learn to bake bread");

		// when
		var result = courseRepository.searchByKeyword("Java");

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("name").containsExactlyInAnyOrder("Java Fundamentals", "Backend Bootcamp");
	}

	@Test
	void searchByKeyword_keywordMatchesNameAndDescriptionOfSameCourse_courseReturnedOnce() {
		// given
		givenCourse("Java Fundamentals", "Learn Java from scratch");

		// when
		var result = courseRepository.searchByKeyword("Java");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).hasFieldOrPropertyWithValue("name", "Java Fundamentals");
	}

	@Test
	void searchByKeyword_matchingCourse_lightDtoFieldsPopulated() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");

		// when
		var result = courseRepository.searchByKeyword("Java");

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).uuid()).isNotNull();
		assertThat(result.get(0)).hasFieldOrPropertyWithValue("name", "Java Fundamentals")
				.hasFieldOrPropertyWithValue("description", "Introductory programming course")
				.hasFieldOrPropertyWithValue("numberOfStudents", 0);
	}

	@Test
	void searchByKeyword_emptyKeyword_allCoursesReturned() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");
		givenCourse("Cooking 101", "Learn to bake bread");

		// when
		var result = courseRepository.searchByKeyword("");

		// then
		assertThat(result).hasSize(2);
		assertThat(result).extracting("name").containsExactlyInAnyOrder("Java Fundamentals", "Cooking 101");
	}

	@Test
	void searchByKeyword_keywordDifferentCase_noMatch() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");

		// when
		var result = courseRepository.searchByKeyword("java");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void searchByKeyword_noCourses_emptyList() {
		// when
		var result = courseRepository.searchByKeyword("Java");

		// then
		assertThat(result).isEmpty();
	}

	@Test
	void searchByKeyword_nullKeyword_treatedAsLiteralAndReturnsNothing() {
		// given
		givenCourse("Java Fundamentals", "Introductory programming course");

		// when
		// null is concatenated into the LIKE pattern as the literal "%null%", so it matches nothing rather than throwing.
		var result = courseRepository.searchByKeyword(null);

		// then
		assertThat(result).isEmpty();
	}

	private void givenCourse(String name, String description) {
		var createTeacherCommand = new CreateTeacherCommand(TEACHER);
		var teacher = new Teacher(createTeacherCommand);
		teacherRepository.save(teacher);
		var createCourseCommand = CreateCourseCommand.builder().name(name).description(description).build();
		var course = new Course(createCourseCommand, teacher.getId());
		courseRepository.save(course);
	}

}
