package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void create_validCommand_quizCreated() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("quiz-title")
                .description("quiz-description")
                .serialNumber(1)
                .text("quiz-text")
                .questions(List.of(new CreateQuestionCommand("q1"), new CreateQuestionCommand("q2")))
                .build();

        // when
        final Quiz quiz = new Quiz(command, 1, course);

        // then
        assertThat(quiz)
                .hasFieldOrPropertyWithValue("title", "quiz-title")
                .hasFieldOrPropertyWithValue("description", "quiz-description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("course", course);
        assertThat(quiz).extracting("questions").asList().hasSize(2);
    }
}
