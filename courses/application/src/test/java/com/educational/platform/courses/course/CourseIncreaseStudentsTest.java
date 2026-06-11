package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Course#increaseNumberOfStudents()} domain method,
 * including multiple increments to verify cumulative behavior.
 */
public class CourseIncreaseStudentsTest {

    @Test
    void increaseNumberOfStudents_fromZero_incrementsToOne() {
        // given
        final Course course = newCourse();

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void increaseNumberOfStudents_multipleTimes_accumulatesCorrectly() {
        // given
        final Course course = newCourse();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }

    private Course newCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }
}
