package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CreateLectureCommand} builder happy-path and direct constructor accessors.
 */
public class CreateLectureCommandBuilderTest {

    @Test
    void builder_allFieldsSet_accessorsReturnCorrectValues() {
        // when
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Introduction to Java")
                .description("Covers basics of Java programming")
                .serialNumber(1)
                .text("Java is a general-purpose language...")
                .build();

        // then
        assertThat(command.getTitle()).isEqualTo("Introduction to Java");
        assertThat(command.getDescription()).isEqualTo("Covers basics of Java programming");
        assertThat(command.getSerialNumber()).isEqualTo(1);
        assertThat(command.getText()).isEqualTo("Java is a general-purpose language...");
    }

    @Test
    void directConstructor_allFieldsSet_accessorsReturnCorrectValues() {
        // when
        final CreateLectureCommand command = new CreateLectureCommand("title", "desc", 5, "content");

        // then
        assertThat(command.getTitle()).isEqualTo("title");
        assertThat(command.getDescription()).isEqualTo("desc");
        assertThat(command.getSerialNumber()).isEqualTo(5);
        assertThat(command.getText()).isEqualTo("content");
    }

    @Test
    void builder_returnsNewBuilderEachTime() {
        // when
        final CreateLectureCommand.CreateLectureCommandBuilder first = CreateLectureCommand.builder();
        final CreateLectureCommand.CreateLectureCommandBuilder second = CreateLectureCommand.builder();

        // then
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void builder_minimalFields_buildsSuccessfully() {
        // when — no fields set except via build()
        final CreateLectureCommand command = CreateLectureCommand.builder().build();

        // then — all fields null
        assertThat(command.getTitle()).isNull();
        assertThat(command.getDescription()).isNull();
        assertThat(command.getSerialNumber()).isNull();
        assertThat(command.getText()).isNull();
    }
}
