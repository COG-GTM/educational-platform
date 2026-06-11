package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Verifies that {@link Course#publish()} from the initial NOT_SENT_FOR_APPROVAL state
 * throws {@link CourseCannotBePublishedException}, and that a successful publish
 * preserves other fields.
 */
public class CoursePublishFromDraftTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void publish_notSentForApproval_courseCannotBePublishedException() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void publish_approved_preservesNameAndDescription() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("Course Name").description("Course Desc").build(), TEACHER_ID);
        course.approve();

        // when
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Course Name")
                .hasFieldOrPropertyWithValue("description", "Course Desc")
                .hasFieldOrPropertyWithValue("teacher", TEACHER_ID);
    }

    @Test
    void publish_approved_statusRemainsDraftBeforePublish() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
        course.approve();

        // verify pre-condition
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT);

        // when
        course.publish();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }
}
