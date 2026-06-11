package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCourseCommandTest {

    @Test
    void builder_allFieldsSet_commandCreated() {
        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("Java Basics")
                .description("Introduction to Java")
                .build();

        // then
        assertThat(sut.name()).isEqualTo("Java Basics");
        assertThat(sut.description()).isEqualTo("Introduction to Java");
        assertThat(sut.curriculumItems()).isNull();
    }

    @Test
    void builder_withCurriculumItems_itemsStored() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("First lecture")
                .serialNumber(1)
                .text("Content")
                .build();

        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("Course")
                .description("Desc")
                .curriculumItems(List.of(lecture))
                .build();

        // then
        assertThat(sut.curriculumItems()).hasSize(1);
    }

    @Test
    void builder_nullName_storedAsNull() {
        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name(null)
                .description("desc")
                .build();

        // then
        assertThat(sut.name()).isNull();
    }

    @Test
    void builder_nullDescription_storedAsNull() {
        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("name")
                .description(null)
                .build();

        // then
        assertThat(sut.description()).isNull();
    }

    @Test
    void builder_emptyStrings_storedAsEmpty() {
        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("")
                .description("")
                .build();

        // then
        assertThat(sut.name()).isEmpty();
        assertThat(sut.description()).isEmpty();
    }

    @Test
    void canonicalConstructor_allFieldsPopulated() {
        // given
        final List<CreateCurriculumItemCommand> items = List.of();

        // when
        final CreateCourseCommand sut = new CreateCourseCommand("Name", "Desc", items);

        // then
        assertThat(sut.name()).isEqualTo("Name");
        assertThat(sut.description()).isEqualTo("Desc");
        assertThat(sut.curriculumItems()).isSameAs(items);
    }

    @Test
    void recordEquality_sameFields_areEqual() {
        // given
        final CreateCourseCommand a = CreateCourseCommand.builder()
                .name("name")
                .description("desc")
                .build();
        final CreateCourseCommand b = CreateCourseCommand.builder()
                .name("name")
                .description("desc")
                .build();

        // then
        assertThat(a).isEqualTo(b);
    }

    @Test
    void recordEquality_differentFields_areNotEqual() {
        // given
        final CreateCourseCommand a = CreateCourseCommand.builder()
                .name("name1")
                .description("desc")
                .build();
        final CreateCourseCommand b = CreateCourseCommand.builder()
                .name("name2")
                .description("desc")
                .build();

        // then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void builder_withMultipleCurriculumItems_allPreserved() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("L1").description("D1").serialNumber(1).text("T1").build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Q1").description("D2").serialNumber(2).text("T2")
                .questions(List.of(new CreateQuestionCommand("Q?")))
                .build();

        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("Course")
                .description("Desc")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // then
        assertThat(sut.curriculumItems()).hasSize(2);
        assertThat(sut.curriculumItems().get(0)).isInstanceOf(CreateLectureCommand.class);
        assertThat(sut.curriculumItems().get(1)).isInstanceOf(CreateQuizCommand.class);
    }
}
