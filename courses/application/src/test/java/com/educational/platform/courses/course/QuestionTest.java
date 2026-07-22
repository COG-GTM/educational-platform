package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuestionTest {

    @Test
    void constructor_contentAndQuiz_questionCreated() {
        // given
        final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), 15);
        final Quiz quiz = new Quiz(CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("question content")))
                .build(), 1, course);

        // when
        final Question question = new Question("content", quiz);

        // then
        assertThat(question)
                .hasFieldOrPropertyWithValue("content", "content")
                .hasFieldOrPropertyWithValue("quiz", quiz);
    }
}
