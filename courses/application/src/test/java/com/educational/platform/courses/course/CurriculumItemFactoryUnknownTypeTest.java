package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CurriculumItemFactory#createFrom} when provided with an unknown
 * subclass of {@link CreateCurriculumItemCommand}. The factory should return null
 * for unrecognized command types.
 */
public class CurriculumItemFactoryUnknownTypeTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void createFrom_unknownCommandType_returnsNull() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
        final CreateCurriculumItemCommand unknownCommand = new CreateCurriculumItemCommand("title", "desc", 1) {};

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(unknownCommand, course);

        // then
        assertThat(result).isNull();
    }

    @Test
    void createFrom_unknownCommandType_nullCourse_returnsNull() {
        // given
        final CreateCurriculumItemCommand unknownCommand = new CreateCurriculumItemCommand("title", "desc", 1) {};

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(unknownCommand, null);

        // then
        assertThat(result).isNull();
    }
}
