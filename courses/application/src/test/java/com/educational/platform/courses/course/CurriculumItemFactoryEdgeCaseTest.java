package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculumItemFactoryEdgeCaseTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void createFrom_lectureCommand_uuidGenerated() {
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
        assertThat(result).hasFieldOrProperty("uuid");
        assertThat(result).extracting("uuid").isNotNull();
    }

    @Test
    void createFrom_quizCommand_questionsPopulated() {
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
                .questions(List.of(
                        new CreateQuestionCommand("question-1"),
                        new CreateQuestionCommand("question-2"),
                        new CreateQuestionCommand("question-3")
                ))
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(result).isInstanceOf(Quiz.class);
        assertThat(result).extracting("questions").asList().hasSize(3);
    }

    @Test
    void createFrom_twoLectures_differentUuids() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateLectureCommand command1 = CreateLectureCommand.builder()
                .title("lecture-1")
                .description("description-1")
                .serialNumber(1)
                .text("content-1")
                .build();
        final CreateLectureCommand command2 = CreateLectureCommand.builder()
                .title("lecture-2")
                .description("description-2")
                .serialNumber(2)
                .text("content-2")
                .build();

        // when
        final CurriculumItem result1 = CurriculumItemFactory.createFrom(command1, course);
        final CurriculumItem result2 = CurriculumItemFactory.createFrom(command2, course);

        // then
        final UUID uuid1 = (UUID) ReflectionTestUtils.getField(result1, "uuid");
        final UUID uuid2 = (UUID) ReflectionTestUtils.getField(result2, "uuid");
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void createFrom_lectureCommand_contentPreserved() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("lecture-title")
                .description("lecture-description")
                .serialNumber(5)
                .text("detailed lecture content")
                .build();

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(command, course);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("content", "detailed lecture content")
                .hasFieldOrPropertyWithValue("serialNumber", 5);
    }
}
