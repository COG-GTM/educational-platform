package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseConstructorTest {

    @Test
    void constructor_validCommandWithoutItems_initializesDefaultValues() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Test Course")
                .description("A test course description")
                .build();

        // when
        final Course course = new Course(command, 10);

        // then
        assertThat(course.toIdentity()).isNotNull();
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Test Course")
                .hasFieldOrPropertyWithValue("description", "A test course description")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0))
                .hasFieldOrPropertyWithValue("teacher", 10);
    }

    @Test
    void constructor_commandWithLecture_courseContainsLecture() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Introduction")
                .serialNumber(1)
                .text("Lecture content")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course With Lectures")
                .description("Has curriculum items")
                .curriculumItems(List.of(lecture))
                .build();

        // when
        final Course course = new Course(command, 5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Course With Lectures")
                .hasFieldOrPropertyWithValue("teacher", 5);
        assertThat(course).extracting("curriculumItems")
                .asList()
                .hasSize(1);
    }

    @Test
    void constructor_commandWithQuiz_courseContainsQuiz() {
        // given
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(1)
                .text("Quiz text")
                .questions(List.of(new CreateQuestionCommand("What is 2+2?")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course With Quiz")
                .description("Has a quiz")
                .curriculumItems(List.of(quiz))
                .build();

        // when
        final Course course = new Course(command, 3);

        // then
        assertThat(course).extracting("curriculumItems")
                .asList()
                .hasSize(1);
    }

    @Test
    void constructor_commandWithMultipleCurriculumItems_courseContainsAll() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("First lecture")
                .serialNumber(1)
                .text("Content")
                .build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(2)
                .text("Quiz text")
                .questions(List.of(new CreateQuestionCommand("Q1?")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Full Course")
                .description("Has both")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, 7);

        // then
        assertThat(course).extracting("curriculumItems")
                .asList()
                .hasSize(2);
    }

    @Test
    void constructor_commandWithNullCurriculumItems_courseHasNullItems() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course No Items")
                .description("No curriculum items")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, 1);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Course No Items")
                .hasFieldOrPropertyWithValue("curriculumItems", null);
    }

    @Test
    void toIdentity_returnsUUID() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, 1);

        // when / then
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void increaseNumberOfStudents_fromZero_becomesOne() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, 1);

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void increaseNumberOfStudents_calledTwice_becomesTwo() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, 1);

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void updateRating_setsNewRating() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, 1);

        // when
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void updateRating_zeroRating_setsZero() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, 1);
        course.updateRating(3.0);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }
}
