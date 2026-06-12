package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCourseCommandTest {

    @Test
    void builder_nameAndDescription_commandCreated() {
        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("Java Course")
                .description("Learn Java")
                .build();

        // then
        assertThat(sut.name()).isEqualTo("Java Course");
        assertThat(sut.description()).isEqualTo("Learn Java");
        assertThat(sut.curriculumItems()).isNull();
    }

    @Test
    void builder_withCurriculumItems_commandCreatedWithItems() {
        // given
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("desc")
                .serialNumber(1)
                .text("content")
                .build();

        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(List.of(lecture))
                .build();

        // then
        assertThat(sut.curriculumItems()).hasSize(1);
    }

    @Test
    void builder_nullFields_commandCreatedWithNulls() {
        // when
        final CreateCourseCommand sut = CreateCourseCommand.builder().build();

        // then
        assertThat(sut.name()).isNull();
        assertThat(sut.description()).isNull();
        assertThat(sut.curriculumItems()).isNull();
    }

    @Test
    void record_directConstructor_fieldsAccessible() {
        // when
        final CreateCourseCommand sut = new CreateCourseCommand("name", "desc", null);

        // then
        assertThat(sut.name()).isEqualTo("name");
        assertThat(sut.description()).isEqualTo("desc");
        assertThat(sut.curriculumItems()).isNull();
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final CreateCourseCommand cmd1 = new CreateCourseCommand("name", "desc", null);
        final CreateCourseCommand cmd2 = new CreateCourseCommand("name", "desc", null);

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentValues_notEqual() {
        // given
        final CreateCourseCommand cmd1 = new CreateCourseCommand("name1", "desc", null);
        final CreateCourseCommand cmd2 = new CreateCourseCommand("name2", "desc", null);

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
