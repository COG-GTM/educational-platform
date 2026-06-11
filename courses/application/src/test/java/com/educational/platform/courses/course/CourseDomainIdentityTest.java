package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link Course#toIdentity()} returns a non-null UUID and that each
 * newly created course receives a distinct identity.
 */
public class CourseDomainIdentityTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void toIdentity_newCourse_returnsNonNullUuid() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);

        // when / then
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void toIdentity_twoCourses_returnDistinctUuids() {
        // given
        final Course course1 = new Course(
                CreateCourseCommand.builder().name("name1").description("desc1").build(), TEACHER_ID);
        final Course course2 = new Course(
                CreateCourseCommand.builder().name("name2").description("desc2").build(), TEACHER_ID);

        // when / then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

    @Test
    void toIdentity_calledMultipleTimes_returnsSameValue() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);

        // when
        final var first = course.toIdentity();
        final var second = course.toIdentity();

        // then
        assertThat(first).isEqualTo(second);
    }
}
