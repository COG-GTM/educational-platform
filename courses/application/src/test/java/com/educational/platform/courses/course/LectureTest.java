package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LectureTest {

    private static final Integer TEACHER_ID = 15;

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("course")
                .description("desc")
                .build();
        return new Course(command, TEACHER_ID);
    }

    @Test
    void constructor_allFields_storedCorrectly() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Lecture Title")
                .description("Lecture Description")
                .serialNumber(1)
                .text("Lecture body content")
                .build();

        // when
        final Lecture sut = new Lecture(command, 1, course);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("title", "Lecture Title")
                .hasFieldOrPropertyWithValue("description", "Lecture Description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("content", "Lecture body content")
                .hasFieldOrPropertyWithValue("course", course);
    }

    @Test
    void constructor_nullContent_storedAsNull() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text(null)
                .build();

        // when
        final Lecture sut = new Lecture(command, 1, course);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", null);
    }

    @Test
    void constructor_emptyContent_storedAsEmpty() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("")
                .build();

        // when
        final Lecture sut = new Lecture(command, 1, course);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("content", "");
    }

    @Test
    void constructor_uuidGenerated() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .build();

        // when
        final Lecture sut = new Lecture(command, 1, course);

        // then
        assertThat(sut).extracting("uuid").isNotNull();
    }

    @Test
    void constructor_serialNumberFromParameter_notFromCommand() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(99)
                .text("text")
                .build();

        // when — serialNumber parameter (5) overrides command's serialNumber (99)
        final Lecture sut = new Lecture(command, 5, course);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("serialNumber", 5);
    }

    @Test
    void constructor_nullTitleAndDescription_storedAsNull() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title(null)
                .description(null)
                .serialNumber(1)
                .text("text")
                .build();

        // when
        final Lecture sut = new Lecture(command, 1, course);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("title", null)
                .hasFieldOrPropertyWithValue("description", null);
    }

    @Test
    void isCurriculumItem() {
        // then
        assertThat(CurriculumItem.class).isAssignableFrom(Lecture.class);
    }

    @Test
    void twoLectures_differentUuids() {
        // given
        final Course course = createCourse();
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Title")
                .description("Desc")
                .serialNumber(1)
                .text("text")
                .build();

        // when
        final Lecture lecture1 = new Lecture(command, 1, course);
        final Lecture lecture2 = new Lecture(command, 2, course);

        // then
        assertThat(lecture1).extracting("uuid").isNotEqualTo(
                org.springframework.test.util.ReflectionTestUtils.getField(lecture2, "uuid"));
    }
}
