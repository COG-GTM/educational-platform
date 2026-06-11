package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Course} constructor with curriculum items, including the null check branch.
 */
public class CourseConstructorCurriculumTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void constructor_withNullCurriculumItems_curriculumItemsIsNull() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        final List<?> items = (List<?>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).isNull();
    }

    @Test
    void constructor_withEmptyCurriculumItems_curriculumItemsIsEmpty() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(List.of())
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        final List<?> items = (List<?>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).isEmpty();
    }

    @Test
    void constructor_withLecture_createsCurriculumItem() {
        // given
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Intro")
                .description("Introduction lecture")
                .serialNumber(1)
                .text("Content text")
                .build();

        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(List.of(lectureCommand))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        final List<?> items = (List<?>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(1);
        assertThat(items.getFirst()).isInstanceOf(Lecture.class);
    }

    @Test
    void constructor_withQuiz_createsCurriculumItem() {
        // given
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(1)
                .questions(List.of(new CreateQuestionCommand("What is Java?")))
                .build();

        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(List.of(quizCommand))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        final List<?> items = (List<?>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(1);
        assertThat(items.getFirst()).isInstanceOf(Quiz.class);
    }

    @Test
    void constructor_withMixedCurriculumItems_allCreated() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture")
                .description("Desc")
                .serialNumber(1)
                .text("Text")
                .build();

        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz")
                .description("Desc")
                .serialNumber(2)
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();

        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        final List<?> items = (List<?>) ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(2);
    }

    @Test
    void constructor_setsNameAndDescription() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Java Basics")
                .description("Learn Java fundamentals")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Java Basics")
                .hasFieldOrPropertyWithValue("description", "Learn Java fundamentals")
                .hasFieldOrPropertyWithValue("teacher", TEACHER_ID);
    }
}
