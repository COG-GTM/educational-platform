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
    void createFrom_createLectureCommand_lectureCreated() {
        // given
        final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), 15);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item)
                .isInstanceOf(Lecture.class)
                .hasFieldOrPropertyWithValue("title", "title")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("content", "text");
    }

    @Test
    void createFrom_createQuizCommand_quizCreated() {
        // given
        final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), 15);
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(2)
                .text("text")
                .questions(List.of(new CreateQuestionCommand("question content")))
                .build();

        // when
        final CurriculumItem item = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(item)
                .isInstanceOf(Quiz.class)
                .hasFieldOrPropertyWithValue("title", "title")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("serialNumber", 2);
    }
}
