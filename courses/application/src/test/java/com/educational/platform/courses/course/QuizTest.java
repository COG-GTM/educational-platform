package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizTest {

    @Test
    void constructor_validCommand_quizCreatedWithQuestions() {
        // given
        final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), 15);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("first question"), new CreateQuestionCommand("second question")))
                .build();

        // when
        final Quiz quiz = new Quiz(command, 1, course);

        // then
        assertThat(quiz)
                .hasFieldOrPropertyWithValue("title", "title")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("course", course);
        @SuppressWarnings("unchecked")
        final List<Question> questions = (List<Question>) ReflectionTestUtils.getField(quiz, "questions");
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0)).hasFieldOrPropertyWithValue("content", "first question");
        assertThat(questions.get(1)).hasFieldOrPropertyWithValue("content", "second question");
    }
}
