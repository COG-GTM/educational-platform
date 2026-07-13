package com.educational.platform.courses.course.create;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCurriculumCommandTest {

    @Test
    void createLectureCommandBuilder_buildsCommandWithAllFields() {
        // when
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .build();

        // then
        assertThat(command.getTitle()).isEqualTo("title");
        assertThat(command.getDescription()).isEqualTo("description");
        assertThat(command.getSerialNumber()).isEqualTo(1);
        assertThat(command.getText()).isEqualTo("text");
    }

    @Test
    void createQuizCommandBuilder_buildsCommandWithAllFields() {
        // given
        final List<CreateQuestionCommand> questions = List.of(new CreateQuestionCommand("question"));

        // when
        final CreateQuizCommand command = CreateQuizCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(2)
                .text("text")
                .questions(questions)
                .build();

        // then
        assertThat(command.getTitle()).isEqualTo("title");
        assertThat(command.getDescription()).isEqualTo("description");
        assertThat(command.getSerialNumber()).isEqualTo(2);
        assertThat(command.getText()).isEqualTo("text");
        assertThat(command.getQuestions()).isEqualTo(questions);
    }

    @Test
    void createQuestionCommand_storesContent() {
        // when
        final CreateQuestionCommand command = new CreateQuestionCommand("content");

        // then
        assertThat(command.content()).isEqualTo("content");
    }

    @Test
    void createCourseCommandBuilder_buildsCommandWithCurriculumItems() {
        // given
        final List<CreateCurriculumItemCommand> items = List.of(
                CreateLectureCommand.builder().title("t").description("d").serialNumber(1).text("x").build());

        // when
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(items)
                .build();

        // then
        assertThat(command.name()).isEqualTo("name");
        assertThat(command.description()).isEqualTo("description");
        assertThat(command.curriculumItems()).isEqualTo(items);
    }
}
