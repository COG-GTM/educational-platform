package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CreateCourseCommand} record accessors, builder pattern, and equality.
 */
public class CreateCourseCommandRecordTest {

    @Test
    void builder_allFieldsSet_accessorsReturnCorrectValues() {
        // when
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Mathematics")
                .description("Advanced math")
                .build();

        // then
        assertThat(command.name()).isEqualTo("Mathematics");
        assertThat(command.description()).isEqualTo("Advanced math");
        assertThat(command.curriculumItems()).isNull();
    }

    @Test
    void builder_withCurriculumItems_setsList() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1").description("Desc").serialNumber(1).text("Text").build();

        // when
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Physics")
                .description("Intro")
                .curriculumItems(List.of(lecture))
                .build();

        // then
        assertThat(command.curriculumItems()).hasSize(1);
    }

    @Test
    void recordEquality_sameValues_equal() {
        // given
        final CreateCourseCommand first = new CreateCourseCommand("name", "desc", null);
        final CreateCourseCommand second = new CreateCourseCommand("name", "desc", null);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void recordEquality_differentValues_notEqual() {
        // given
        final CreateCourseCommand first = new CreateCourseCommand("A", "desc", null);
        final CreateCourseCommand second = new CreateCourseCommand("B", "desc", null);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void builder_minimalFields_buildsSuccessfully() {
        // when
        final CreateCourseCommand command = CreateCourseCommand.builder().build();

        // then
        assertThat(command.name()).isNull();
        assertThat(command.description()).isNull();
        assertThat(command.curriculumItems()).isNull();
    }

    @Test
    void builder_returnsNewBuilderEachTime() {
        // when
        final CreateCourseCommand.CreateCourseCommandBuilder b1 = CreateCourseCommand.builder();
        final CreateCourseCommand.CreateCourseCommandBuilder b2 = CreateCourseCommand.builder();

        // then
        assertThat(b1).isNotSameAs(b2);
    }
}
