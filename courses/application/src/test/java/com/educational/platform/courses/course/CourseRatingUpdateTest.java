package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRatingUpdateTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void updateRating_positiveValue_ratingUpdated() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void updateRating_zero_ratingSetToZero() {
        // given
        final Course course = createCourse();
        course.updateRating(3.0);

        // when
        course.updateRating(0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0));
    }

    @Test
    void updateRating_maxValue_ratingUpdated() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(5.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void increaseNumberOfStudents_fromZero_numberIsOne() {
        // given
        final Course course = createCourse();

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void increaseNumberOfStudents_calledMultipleTimes_numberIncrements() {
        // given
        final Course course = createCourse();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }

    @Test
    void toIdentity_afterCreation_uuidIsNotNull() {
        // given
        final Course course = createCourse();

        // when
        final var uuid = course.toIdentity();

        // then
        assertThat(uuid).isNotNull();
    }

    @Test
    void toIdentity_twoCourses_differentUuids() {
        // given
        final Course course1 = createCourse();
        final Course course2 = createCourse();

        // when / then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Test Course")
                .description("Description")
                .build();
        return new Course(command, TEACHER_ID);
    }
}
