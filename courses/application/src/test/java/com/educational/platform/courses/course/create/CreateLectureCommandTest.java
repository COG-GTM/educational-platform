package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateLectureCommandTest {

    @Test
    void builder_allFieldsSet_commandCreatedWithCorrectValues() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("Introduction")
                .description("First lecture")
                .serialNumber(1)
                .text("Lecture content")
                .build();

        // then
        assertThat(sut.getTitle()).isEqualTo("Introduction");
        assertThat(sut.getDescription()).isEqualTo("First lecture");
        assertThat(sut.getSerialNumber()).isEqualTo(1);
        assertThat(sut.getText()).isEqualTo("Lecture content");
    }

    @Test
    void builder_nullFields_commandCreatedWithNulls() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder().build();

        // then
        assertThat(sut.getTitle()).isNull();
        assertThat(sut.getDescription()).isNull();
        assertThat(sut.getSerialNumber()).isNull();
        assertThat(sut.getText()).isNull();
    }

    @Test
    void constructor_directInstantiation_fieldsAccessible() {
        // when
        final CreateLectureCommand sut = new CreateLectureCommand("title", "desc", 5, "content");

        // then
        assertThat(sut.getTitle()).isEqualTo("title");
        assertThat(sut.getDescription()).isEqualTo("desc");
        assertThat(sut.getSerialNumber()).isEqualTo(5);
        assertThat(sut.getText()).isEqualTo("content");
    }

    @Test
    void builder_isSubclassOfCreateCurriculumItemCommand() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("title")
                .build();

        // then
        assertThat(sut).isInstanceOf(CreateCurriculumItemCommand.class);
    }
}
