package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateLectureCommandTest {

    @Test
    void builder_allFieldsSet_commandCreated() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("Lecture Title")
                .description("Lecture Description")
                .serialNumber(1)
                .text("Lecture body content")
                .build();

        // then
        assertThat(sut.getTitle()).isEqualTo("Lecture Title");
        assertThat(sut.getDescription()).isEqualTo("Lecture Description");
        assertThat(sut.getSerialNumber()).isEqualTo(1);
        assertThat(sut.getText()).isEqualTo("Lecture body content");
    }

    @Test
    void constructor_allFieldsPopulated() {
        // when
        final CreateLectureCommand sut = new CreateLectureCommand("Title", "Desc", 3, "Text");

        // then
        assertThat(sut.getTitle()).isEqualTo("Title");
        assertThat(sut.getDescription()).isEqualTo("Desc");
        assertThat(sut.getSerialNumber()).isEqualTo(3);
        assertThat(sut.getText()).isEqualTo("Text");
    }

    @Test
    void builder_nullText_storedAsNull() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text(null)
                .build();

        // then
        assertThat(sut.getText()).isNull();
    }

    @Test
    void builder_nullSerialNumber_storedAsNull() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(null)
                .text("Text")
                .build();

        // then
        assertThat(sut.getSerialNumber()).isNull();
    }

    @Test
    void extendsCreateCurriculumItemCommand() {
        // then
        assertThat(CreateCurriculumItemCommand.class).isAssignableFrom(CreateLectureCommand.class);
    }

    @Test
    void builder_emptyStrings_storedAsEmpty() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("")
                .description("")
                .serialNumber(0)
                .text("")
                .build();

        // then
        assertThat(sut.getTitle()).isEmpty();
        assertThat(sut.getDescription()).isEmpty();
        assertThat(sut.getText()).isEmpty();
    }

    @Test
    void builder_zeroSerialNumber_storedCorrectly() {
        // when
        final CreateLectureCommand sut = CreateLectureCommand.builder()
                .title("T")
                .description("D")
                .serialNumber(0)
                .text("X")
                .build();

        // then
        assertThat(sut.getSerialNumber()).isEqualTo(0);
    }
}
