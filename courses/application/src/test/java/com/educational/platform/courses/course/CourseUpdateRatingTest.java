package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Course#updateRating(double)} domain method,
 * including boundary values and overwrite behavior.
 */
public class CourseUpdateRatingTest {

    @Test
    void updateRating_setsNewRating() {
        // given
        final Course course = newCourse();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void updateRating_overwritesPreviousRating() {
        // given
        final Course course = newCourse();
        course.updateRating(3.0);

        // when
        course.updateRating(4.8);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.8));
    }

    @Test
    void updateRating_zeroRating() {
        // given
        final Course course = newCourse();
        course.updateRating(5.0);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void updateRating_negativeRating_accepted() {
        // given
        final Course course = newCourse();

        // when
        course.updateRating(-1.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(-1.0));
    }

    private Course newCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }
}
