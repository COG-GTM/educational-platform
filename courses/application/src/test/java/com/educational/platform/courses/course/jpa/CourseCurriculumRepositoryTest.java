package com.educational.platform.courses.course.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.course.details.CurriculumItemSummaryDTO;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

/**
 * Persists lectures and quizzes, which the default embedded test database cannot do: Hibernate generates a
 * {@code check (type in ('Lecture','Quiz'))} discriminator constraint whose IN-list keeps a reference to the H2 session
 * that ran the DDL, and the non-pooled embedded data source closes that session right after schema export, so every
 * later insert fails with "Check constraint invalid". A pooled data source keeps the DDL connection open instead.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:course-curriculum;DB_CLOSE_DELAY=-1")
public class CourseCurriculumRepositoryTest {

	private static final String TEACHER = "teacher";

	@Autowired
	private CourseRepository courseRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Test
	void findCurriculum_lectureAndQuiz_orderedBySerialNumberWithType() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var quiz = CreateQuizCommand.builder().title("Intro quiz").description("quiz description").serialNumber(2)
				.questions(List.of(new CreateQuestionCommand("question"))).build();
		var lecture = CreateLectureCommand.builder().title("Intro lecture").description("lecture description").serialNumber(1).text("content").build();
		var closing = CreateLectureCommand.builder().title("Closing lecture").description(null).serialNumber(3).text("content").build();
		var course = new Course(CreateCourseCommand.builder().name("name").description("description").curriculumItems(List.of(quiz, closing, lecture)).build(), teacher.getId());
		courseRepository.save(course);

		// when
		var result = courseRepository.findCurriculum(course.toIdentity());

		// then
		assertThat(result).hasSize(3);
		assertThat(result).extracting(CurriculumItemSummaryDTO::serialNumber).containsExactly(1, 2, 3);
		assertThat(result).extracting(CurriculumItemSummaryDTO::title).containsExactly("Intro lecture", "Intro quiz", "Closing lecture");
		assertThat(result).extracting(CurriculumItemSummaryDTO::type).containsExactly("LECTURE", "QUIZ", "LECTURE");
		assertThat(result).extracting(CurriculumItemSummaryDTO::description).containsExactly("lecture description", "quiz description", null);
		assertThat(result).extracting(CurriculumItemSummaryDTO::uuid).doesNotContainNull().doesNotHaveDuplicates();
	}

	@Test
	void findCurriculum_itemsOfOtherCourse_notIncluded() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var lecture = CreateLectureCommand.builder().title("Intro lecture").description("lecture description").serialNumber(1).text("content").build();
		var course = courseRepository.save(new Course(CreateCourseCommand.builder().name("name").description("description").curriculumItems(List.of(lecture)).build(), teacher.getId()));
		var otherLecture = CreateLectureCommand.builder().title("Other lecture").description("other").serialNumber(1).text("content").build();
		courseRepository.save(new Course(CreateCourseCommand.builder().name("other").description("other").curriculumItems(List.of(otherLecture)).build(), teacher.getId()));

		// when
		var result = courseRepository.findCurriculum(course.toIdentity());

		// then
		assertThat(result).extracting(CurriculumItemSummaryDTO::title).containsExactly("Intro lecture");
	}

	@Test
	void findCurriculum_courseWithoutItems_empty() {
		// given
		var teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand(TEACHER)));
		var course = courseRepository.save(new Course(CreateCourseCommand.builder().name("name").description("description").build(), teacher.getId()));

		// when
		var result = courseRepository.findCurriculum(course.toIdentity());

		// then
		assertThat(result).isEmpty();
	}

}
