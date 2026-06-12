package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LectureTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void create_validCommand_lectureCreated() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateLectureCommand command = CreateLectureCommand.builder()
                .title("lecture-title")
                .description("lecture-description")
                .serialNumber(1)
                .text("lecture-content")
                .build();

        // when
        final Lecture lecture = new Lecture(command, 1, course);

        // then
        assertThat(lecture)
                .hasFieldOrPropertyWithValue("title", "lecture-title")
                .hasFieldOrPropertyWithValue("description", "lecture-description")
                .hasFieldOrPropertyWithValue("serialNumber", 1)
                .hasFieldOrPropertyWithValue("content", "lecture-content")
                .hasFieldOrPropertyWithValue("course", course);
    }
}
