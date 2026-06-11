package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseCurriculumItemsTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void constructor_withLectureItems_createsCurriculumItems() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("First lecture")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Java Course")
                .description("Learn Java")
                .curriculumItems(List.of(lecture))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        @SuppressWarnings("unchecked")
        final List<CurriculumItem> items = (List<CurriculumItem>)
                org.springframework.test.util.ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(1);
        assertThat(items.getFirst()).isInstanceOf(Lecture.class);
    }

    @Test
    void constructor_withQuizItems_createsCurriculumItems() {
        // given
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(1)
                .text("Quiz intro")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Java Course")
                .description("Learn Java")
                .curriculumItems(List.of(quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        @SuppressWarnings("unchecked")
        final List<CurriculumItem> items = (List<CurriculumItem>)
                org.springframework.test.util.ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(1);
        assertThat(items.getFirst()).isInstanceOf(Quiz.class);
    }

    @Test
    void constructor_withMixedItems_createsAllCurriculumItems() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture").description("d").serialNumber(1).text("t").build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz").description("d").serialNumber(2).text("t")
                .questions(List.of(new CreateQuestionCommand("Q")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Desc")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        @SuppressWarnings("unchecked")
        final List<CurriculumItem> items = (List<CurriculumItem>)
                org.springframework.test.util.ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).hasSize(2);
        assertThat(items).hasAtLeastOneElementOfType(Lecture.class);
        assertThat(items).hasAtLeastOneElementOfType(Quiz.class);
    }

    @Test
    void constructor_withNullCurriculumItems_leavesItemsNull() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Desc")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        final Object items = org.springframework.test.util.ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).isNull();
    }

    @Test
    void constructor_withEmptyList_createsEmptyCurriculumItems() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Desc")
                .curriculumItems(List.of())
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        @SuppressWarnings("unchecked")
        final List<CurriculumItem> items = (List<CurriculumItem>)
                org.springframework.test.util.ReflectionTestUtils.getField(course, "curriculumItems");
        assertThat(items).isEmpty();
    }
}
