package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumItemFactoryTest {

    private Course course;

    @BeforeEach
    void setUp() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        course = new Course(command, 1);
    }

    @Test
    void createFrom_lectureCommand_createsLecture() {
        // given
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item).isInstanceOf(Lecture.class);
    }

    @Test
    void createFrom_quizCommand_createsQuiz() {
        // given
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("content")))
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item).isInstanceOf(Quiz.class);
    }

    @Test
    void createFrom_unknownCommand_returnsNull() {
        // given
        final CreateCurriculumItemCommand command = new CreateCurriculumItemCommand("title", "description", 1) {
        };

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item).isNull();
    }
}
