package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LectureTest {

    @Test
    void constructor_validCommand_lectureCreated() {
        // given
        final Course course = new Course(CreateCourseCommand.builder().name("name").description("description").build(), 15);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("title")
                .description("description")
                .serialNumber(1)
                .text("text")
                .build();

        // when
        final Lecture lecture = new Lecture(command, 1, course);

        // then
        assertThat(lecture)
                .hasFieldOrPropertyWithValue("title", "title")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("content", "text")
                .hasFieldOrPropertyWithValue("course", course);
    }
}
