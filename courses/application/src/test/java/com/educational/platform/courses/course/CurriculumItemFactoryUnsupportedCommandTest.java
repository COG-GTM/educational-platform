package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CurriculumItemFactoryUnsupportedCommandTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void createFrom_unsupportedCommandType_returnsNull() {
        // given
        final CreateCourseCommand courseCommand = CreateCourseCommand.builder()
                .name("course-name")
                .description("course-description")
                .build();
        final Course course = new Course(courseCommand, TEACHER_ID);
        final CreateCurriculumItemCommand unsupported = new CreateCurriculumItemCommand("title", "desc", 1) {
        };

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(unsupported, course);

        // then
        assertThat(result).isNull();
    }
}
