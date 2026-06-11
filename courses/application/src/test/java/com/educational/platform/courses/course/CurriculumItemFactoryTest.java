package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumItemFactoryTest {

    @Test
    void createFrom_lectureCommand_returnsLectureInstance() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("Lecture title")
                .description("Lecture description")
                .serialNumber(1)
                .text("Lecture text content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(lectureCommand, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
    }

    @Test
    void createFrom_quizCommand_returnsQuizInstance() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Quiz title")
                .description("Quiz description")
                .serialNumber(2)
                .text("Quiz instructions")
                .questions(List.of())
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
    }

    @Test
    void createFrom_quizCommandWithQuestions_returnsQuizInstance() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        final CreateQuestionCommand question = new CreateQuestionCommand("What is 2+2?");
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("Math Quiz")
                .description("Basic math")
                .serialNumber(1)
                .text("Answer all questions")
                .questions(List.of(question))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(quizCommand, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
    }
}
