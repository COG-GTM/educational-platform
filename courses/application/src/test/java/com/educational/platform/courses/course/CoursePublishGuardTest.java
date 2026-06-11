package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the {@link Course#publish()} domain guard clause: only approved courses
 * may be published. Covers all non-approved states.
 */
public class CoursePublishGuardTest {

    @Test
    void publish_approvedCourse_transitionsToPublished() {
        // given
        final Course course = newCourse();
        course.approve();

        // when
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void publish_draftCourse_throwsCourseCannotBePublishedException() {
        // given
        final Course course = newCourse();

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(course::publish);
    }

    @Test
    void publish_declinedCourse_throwsCourseCannotBePublishedException() {
        // given
        final Course course = newCourse();
        course.decline();

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(course::publish);
    }

    @Test
    void publish_waitingForApprovalCourse_throwsCourseCannotBePublishedException() {
        // given
        final Course course = newCourse();
        course.sendToApprove();

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(course::publish);
    }

    private Course newCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }
}
