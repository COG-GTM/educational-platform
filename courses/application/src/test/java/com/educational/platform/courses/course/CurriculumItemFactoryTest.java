package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CurriculumItemFactoryTest {

    @Test
    void createFrom_lectureCommand_lectureMappedWithContentAndCourse() {
        // given
        final Course course = mock(Course.class);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Intro")
                .description("intro description")
                .serialNumber(1)
                .text("lecture body")
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item)
                .isInstanceOf(Lecture.class)
                .hasFieldOrPropertyWithValue("title", "Intro")
                .hasFieldOrPropertyWithValue("description", "intro description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("course", course)
                .hasFieldOrPropertyWithValue("content", "lecture body");
    }

    @Test
    void createFrom_quizCommand_quizMappedWithQuestionsAndCourse() {
        // given
        final Course course = mock(Course.class);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("quiz description")
                .serialNumber(2)
                .text("quiz body")
                .questions(List.of(new CreateQuestionCommand("Q1"), new CreateQuestionCommand("Q2")))
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item)
                .isInstanceOf(Quiz.class)
                .hasFieldOrPropertyWithValue("title", "Quiz 1")
                .hasFieldOrPropertyWithValue("description", "quiz description")
                .hasFieldOrPropertyWithValue("serialNumber", 2)
                .hasFieldOrPropertyWithValue("course", course);

        final List<?> questions = (List<?>) ReflectionTestUtils.getField(item, "questions");
        assertThat(questions)
                .hasSize(2)
                .extracting("content")
                .containsExactly("Q1", "Q2");
        assertThat(questions)
                .extracting("quiz")
                .containsOnly(item);
    }

    @Test
    void createFrom_emptyQuizQuestions_quizMappedWithoutQuestions() {
        // given - a quiz with no questions still maps to a Quiz, the question list is just empty
        final Course course = mock(Course.class);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("Empty Quiz")
                .description("quiz description")
                .serialNumber(3)
                .text("quiz body")
                .questions(List.of())
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item).isInstanceOf(Quiz.class);
        assertThat((List<?>) ReflectionTestUtils.getField(item, "questions")).isEmpty();
    }

    @Test
    void createFrom_unknownCommandType_returnsNull() {
        // given - the factory only understands lecture and quiz commands; any other curriculum item type maps to null
        final CreateCurriculumItemCommand command =
                new UnsupportedCurriculumItemCommand("title", "description", 1);

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, mock(Course.class));

        // then
        assertThat(item).isNull();
    }

    private static final class UnsupportedCurriculumItemCommand extends CreateCurriculumItemCommand {
        private UnsupportedCurriculumItemCommand(String title, String description, Integer serialNumber) {
            super(title, description, serialNumber);
        }
    }
}
