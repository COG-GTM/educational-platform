package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests sequential {@link Course#increaseNumberOfStudents()} calls
 * to verify correct accumulation behavior.
 */
public class CourseMultipleIncrementStudentsTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void increaseNumberOfStudents_fiveSequentialCalls_incrementsToFive() {
        // given
        final Course course = createCourse();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(5));
    }

    @Test
    void increaseNumberOfStudents_afterRatingUpdate_independentOfRating() {
        // given
        final Course course = createCourse();
        course.updateRating(4.5);

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then — student count and rating are independent
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3))
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void increaseNumberOfStudents_afterArchive_stillIncrements() {
        // given
        final Course course = createCourse();
        course.archive();

        // when
        course.increaseNumberOfStudents();

        // then — archiving does not prevent enrollment counter from increasing
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1))
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    private Course createCourse() {
        return new Course(
                CreateCourseCommand.builder().name("Course").description("desc").build(),
                TEACHER_ID);
    }
}
