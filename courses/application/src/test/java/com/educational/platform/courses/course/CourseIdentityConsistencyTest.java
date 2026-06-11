package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link Course#toIdentity()} returns a stable UUID across multiple
 * invocations on the same instance and differs between distinct instances.
 */
public class CourseIdentityConsistencyTest {

    private static final Integer TEACHER_ID = 1;

    private Course createCourse() {
        return new Course(CreateCourseCommand.builder()
                .name("Course").description("Description").build(), TEACHER_ID);
    }

    @Test
    void toIdentity_calledMultipleTimes_returnsSameUuid() {
        // given
        final Course course = createCourse();

        // when
        final UUID first = course.toIdentity();
        final UUID second = course.toIdentity();
        final UUID third = course.toIdentity();

        // then
        assertThat(first).isEqualTo(second).isEqualTo(third);
    }

    @Test
    void toIdentity_differentInstances_returnDifferentUuids() {
        // given
        final Course courseA = createCourse();
        final Course courseB = createCourse();

        // then
        assertThat(courseA.toIdentity()).isNotEqualTo(courseB.toIdentity());
    }

    @Test
    void toIdentity_returnsValidUuid() {
        // given
        final Course course = createCourse();

        // when
        final UUID uuid = course.toIdentity();

        // then
        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(4);
    }
}
