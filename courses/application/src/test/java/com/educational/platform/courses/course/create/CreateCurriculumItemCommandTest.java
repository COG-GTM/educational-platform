package com.educational.platform.courses.course.create;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCurriculumItemCommandTest {

    @Test
    void getTitle_returnsStoredTitle() {
        // given — use CreateLectureCommand as a concrete subclass
        final CreateLectureCommand sut = new CreateLectureCommand("Lecture Title", "Desc", 1, "text");

        // then
        assertThat(sut.getTitle()).isEqualTo("Lecture Title");
    }

    @Test
    void getDescription_returnsStoredDescription() {
        // given
        final CreateLectureCommand sut = new CreateLectureCommand("Title", "Lecture Description", 1, "text");

        // then
        assertThat(sut.getDescription()).isEqualTo("Lecture Description");
    }

    @Test
    void getSerialNumber_returnsStoredSerialNumber() {
        // given
        final CreateLectureCommand sut = new CreateLectureCommand("Title", "Desc", 42, "text");

        // then
        assertThat(sut.getSerialNumber()).isEqualTo(42);
    }

    @Test
    void getTitle_nullTitle_returnsNull() {
        // given
        final CreateLectureCommand sut = new CreateLectureCommand(null, "Desc", 1, "text");

        // then
        assertThat(sut.getTitle()).isNull();
    }

    @Test
    void getDescription_nullDescription_returnsNull() {
        // given
        final CreateLectureCommand sut = new CreateLectureCommand("Title", null, 1, "text");

        // then
        assertThat(sut.getDescription()).isNull();
    }

    @Test
    void getSerialNumber_nullSerialNumber_returnsNull() {
        // given
        final CreateLectureCommand sut = new CreateLectureCommand("Title", "Desc", null, "text");

        // then
        assertThat(sut.getSerialNumber()).isNull();
    }

    @Test
    void getTitle_emptyString_returnsEmpty() {
        // given
        final CreateLectureCommand sut = new CreateLectureCommand("", "Desc", 1, "text");

        // then
        assertThat(sut.getTitle()).isEmpty();
    }

    @Test
    void isAbstractClass() {
        // then
        assertThat(java.lang.reflect.Modifier.isAbstract(CreateCurriculumItemCommand.class.getModifiers())).isTrue();
    }

    @Test
    void createLectureCommand_isSubclass() {
        // then
        assertThat(CreateCurriculumItemCommand.class).isAssignableFrom(CreateLectureCommand.class);
    }

    @Test
    void createQuizCommand_isSubclass() {
        // then
        assertThat(CreateCurriculumItemCommand.class).isAssignableFrom(CreateQuizCommand.class);
    }
}
