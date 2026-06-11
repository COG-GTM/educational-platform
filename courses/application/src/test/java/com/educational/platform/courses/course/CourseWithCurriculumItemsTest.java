package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Course construction with curriculum items (lectures and quizzes),
 * covering the branch where curriculumItems is non-null in the constructor.
 */
public class CourseWithCurriculumItemsTest {

    @Test
    void constructor_withLecture_courseContainsLectureCurriculumItem() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Introduction")
                .serialNumber(1)
                .text("Welcome to the course")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("CS 101")
                .description("Intro to CS")
                .curriculumItems(List.of(lecture))
                .build();

        // when
        final Course course = new Course(command, 1);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "CS 101")
                .hasFieldOrPropertyWithValue("description", "Intro to CS");
        assertThat(course).extracting("curriculumItems").asList().hasSize(1);
    }

    @Test
    void constructor_withQuiz_courseContainsQuizCurriculumItem() {
        // given
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("Assessment")
                .serialNumber(1)
                .text("Answer the following")
                .questions(List.of(new CreateQuestionCommand("What is 1+1?")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Math 101")
                .description("Intro to Math")
                .curriculumItems(List.of(quiz))
                .build();

        // when
        final Course course = new Course(command, 1);

        // then
        assertThat(course).extracting("curriculumItems").asList().hasSize(1);
    }

    @Test
    void constructor_withMultipleCurriculumItems_allCreated() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("Intro")
                .serialNumber(1)
                .text("text")
                .build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("Assessment")
                .serialNumber(2)
                .text("quiz text")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("CS 201")
                .description("Advanced CS")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, 1);

        // then
        assertThat(course).extracting("curriculumItems").asList().hasSize(2);
    }

    @Test
    void constructor_nullCurriculumItems_courseCreatedSuccessfully() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("History 101")
                .description("World History")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, 1);

        // then
        assertThat(course.toIdentity()).isNotNull();
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "History 101")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }
}
