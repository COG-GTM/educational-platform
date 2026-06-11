package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LectureTest {

    @Test
    void constructor_initializesFieldsFromCommand() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("course").description("desc").build(), 1);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("Intro to Java")
                .description("First lecture")
                .serialNumber(1)
                .text("Lecture content text")
                .build();

        // when
        final Lecture lecture = new Lecture(command, command.getSerialNumber(), course);

        // then
        assertThat(lecture)
                .hasFieldOrPropertyWithValue("title", "Intro to Java")
                .hasFieldOrPropertyWithValue("description", "First lecture")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("content", "Lecture content text")
                .hasFieldOrPropertyWithValue("course", course);
        assertThat(lecture).hasFieldOrProperty("uuid");
    }

    @Test
    void constructor_generatesUniqueUuid() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("t").description("d").serialNumber(1).text("x").build();

        // when
        final Lecture lecture1 = new Lecture(command, 1, course);
        final Lecture lecture2 = new Lecture(command, 2, course);

        // then
        final Object uuid1 = org.springframework.test.util.ReflectionTestUtils.getField(lecture1, "uuid");
        final Object uuid2 = org.springframework.test.util.ReflectionTestUtils.getField(lecture2, "uuid");
        assertThat(uuid1).isNotNull();
        assertThat(uuid2).isNotNull();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void constructor_isCurriculumItem() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("c").description("d").build(), 1);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("t").description("d").serialNumber(1).text("x").build();

        // when
        final CurriculumItem item = new Lecture(command, 1, course);

        // then
        assertThat(item).isInstanceOf(Lecture.class);
        assertThat(item).isInstanceOf(CurriculumItem.class);
    }
}
