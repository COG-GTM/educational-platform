package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests boundary/edge-case values for {@link Course} domain behavior.
 */
public class CourseBoundaryValuesTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void updateRating_zero_ratingSetToZero() {
        // given
        final Course course = createCourse();
        course.updateRating(3.5);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void updateRating_negativeValue_accepted() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(-1.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(-1.0));
    }

    @Test
    void updateRating_maxValue_accepted() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(5.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void increaseNumberOfStudents_fromZero_incrementsToOne() {
        // given
        final Course course = createCourse();
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void numberOfStudents_zeroValueRecord_accessorReturnsZero() {
        assertThat(new NumberOfStudents(0).number()).isZero();
    }

    @Test
    void courseRating_zeroValueRecord_accessorReturnsZero() {
        assertThat(new CourseRating(0.0).rating()).isZero();
    }

    @Test
    void courseRating_negativeRecord_accessorReturnsNegative() {
        assertThat(new CourseRating(-1.5).rating()).isEqualTo(-1.5);
    }

    @Test
    void constructor_initialRatingAndStudents_zeroed() {
        // when
        final Course course = createCourse();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    @Test
    void toIdentity_afterCreation_notNull() {
        // when
        final Course course = createCourse();

        // then
        assertThat(course.toIdentity()).isNotNull();
    }

    private Course createCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
    }
}
