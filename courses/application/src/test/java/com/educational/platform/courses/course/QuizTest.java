package com.educational.platform.courses.course;

import java.util.List;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuizTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void createQuiz_validCommand_quizWithQuestionsCreated() {
        // given
        final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), TEACHER_ID);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("quiz title")
                .description("quiz description")
                .serialNumber(2)
                .text("quiz text")
                .questions(List.of(new CreateQuestionCommand("first question"), new CreateQuestionCommand("second question")))
                .build();

        // when
        final Quiz quiz = new Quiz(command, 2, course);

        // then
        assertThat(quiz)
                .hasFieldOrPropertyWithValue("title", "quiz title")
                .hasFieldOrPropertyWithValue("description", "quiz description")
                .hasFieldOrPropertyWithValue("serialNumber", 2)
                .hasFieldOrPropertyWithValue("course", course);

        @SuppressWarnings("unchecked")
        final List<Question> questions = (List<Question>) org.springframework.test.util.ReflectionTestUtils.getField(quiz, "questions");
        assertThat(questions).hasSize(2);
        assertThat(questions.get(0)).hasFieldOrPropertyWithValue("content", "first question").hasFieldOrPropertyWithValue("quiz", quiz);
        assertThat(questions.get(1)).hasFieldOrPropertyWithValue("content", "second question");
    }
}
