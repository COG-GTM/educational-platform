package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void create_validArguments_questionCreated() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("quiz-title")
                .description("quiz-description")
                .serialNumber(1)
                .text("quiz-text")
                .questions(List.of(new CreateQuestionCommand("q1")))
                .build();
        final Quiz quiz = new Quiz(quizCommand, 1, course);

        // when
        final Question question = new Question("question-content", quiz);

        // then
        assertThat(question)
                .hasFieldOrPropertyWithValue("content", "question-content")
                .hasFieldOrPropertyWithValue("quiz", quiz);
    }
}
