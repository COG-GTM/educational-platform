package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumItemFactoryTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void createFrom_lectureCommand_lectureCreated() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("lecture-title")
                .description("lecture-description")
                .serialNumber(1)
                .text("lecture-content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(result).isInstanceOf(Lecture.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "lecture-title")
                .hasFieldOrPropertyWithValue("description", "lecture-description")
                .hasFieldOrPropertyWithValue("serialNumber", 1);
    }

    @Test
    void createFrom_quizCommand_quizCreated() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("quiz-title")
                .description("quiz-description")
                .serialNumber(2)
                .text("quiz-text")
                .questions(List.of(new CreateQuestionCommand("q1"), new CreateQuestionCommand("q2")))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result)
                .hasFieldOrPropertyWithValue("title", "quiz-title")
                .hasFieldOrPropertyWithValue("description", "quiz-description")
                .hasFieldOrPropertyWithValue("serialNumber", 2);
    }
}
