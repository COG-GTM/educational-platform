package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateCurriculumItemCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CurriculumItemFactory#createFrom(CreateCurriculumItemCommand, Course)}
 * when an unsupported command subclass is provided.
 */
public class CurriculumItemFactoryUnknownCommandTest {

    @Test
    void createFrom_unknownCommandSubclass_returnsNull() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        final CreateCurriculumItemCommand unknownCommand = new CreateCurriculumItemCommand("title", "desc", 1) {
        };

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(unknownCommand, course);

        // then
        assertThat(result).isNull();
    }

    @Test
    void createFrom_unknownCommandWithNullFields_returnsNull() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        final CreateCurriculumItemCommand unknownCommand = new CreateCurriculumItemCommand(null, null, null) {
        };

        // when
        final CurriculumItem result = CurriculumItemFactory.createFrom(unknownCommand, course);

        // then
        assertThat(result).isNull();
    }
}
