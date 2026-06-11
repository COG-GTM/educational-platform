package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCurriculumItemsTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void constructor_withLectureCurriculumItems_createsCourseWithLectures() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Introduction")
                .serialNumber(1)
                .text("Lecture content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course with lectures")
                .description("A course containing lectures")
                .curriculumItems(List.of(lecture))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Course with lectures")
                .hasFieldOrPropertyWithValue("description", "A course containing lectures");
        assertThat(course).extracting("curriculumItems")
                .asList()
                .hasSize(1);
    }

    @Test
    void constructor_withQuizCurriculumItems_createsCourseWithQuizzes() {
        // given
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(1)
                .text("Quiz instructions")
                .questions(List.of())
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course with quiz")
                .description("A course containing a quiz")
                .curriculumItems(List.of(quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course).extracting("curriculumItems")
                .asList()
                .hasSize(1);
    }

    @Test
    void constructor_withMultipleCurriculumItems_createsAllItems() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Intro")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("Assessment")
                .serialNumber(2)
                .text("Instructions")
                .questions(List.of())
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Full course")
                .description("Course with mixed items")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course).extracting("curriculumItems")
                .asList()
                .hasSize(2);
    }

    @Test
    void constructor_withNullCurriculumItems_createsNullList() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Simple course")
                .description("No curriculum items")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Simple course")
                .hasFieldOrPropertyWithValue("curriculumItems", null);
    }

    @Test
    void constructor_withEmptyCurriculumItems_createsEmptyList() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Empty curriculum course")
                .description("Course with no items")
                .curriculumItems(List.of())
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course).extracting("curriculumItems")
                .asList()
                .isEmpty();
    }
}
