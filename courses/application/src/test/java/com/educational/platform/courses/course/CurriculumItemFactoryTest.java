package com.educational.platform.courses.course;

import java.util.List;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CurriculumItemFactoryTest {

    private static final Integer TEACHER_ID = 15;

    private final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), TEACHER_ID);

    @Test
    void createFrom_lectureCommand_lectureCreated() {
        // given
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title").description("description").serialNumber(1).text("text").build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item).isInstanceOf(Lecture.class);
        assertThat(item).hasFieldOrPropertyWithValue("title", "title").hasFieldOrPropertyWithValue("content", "text");
    }

    @Test
    void createFrom_quizCommand_quizCreated() {
        // given
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title").description("description").serialNumber(1).text("text")
                .questions(List.of(new CreateQuestionCommand("question"))).build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item).isInstanceOf(Quiz.class);
        assertThat(item).hasFieldOrPropertyWithValue("title", "title");
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
