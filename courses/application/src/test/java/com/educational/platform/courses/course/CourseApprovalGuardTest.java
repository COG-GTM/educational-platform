package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the sendToApprove guard: once a course is approved it cannot be re-sent.
 * Also covers the publish guard for NOT_SENT_FOR_APPROVAL status.
 */
public class CourseApprovalGuardTest {

    private Course createDraftCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }

    @Test
    void sendToApprove_alreadyApproved_courseAlreadyApprovedException() {
        // given
        final Course course = createDraftCourse();
        course.approve();

        // when
        final ThrowableAssert.ThrowingCallable action = course::sendToApprove;

        // then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class).isThrownBy(action);
    }

    @Test
    void publish_notSentForApproval_courseCannotBePublishedException() {
        // given — course is in initial NOT_SENT_FOR_APPROVAL state
        final Course course = createDraftCourse();

        // when
        final ThrowableAssert.ThrowingCallable action = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(action);
    }

    @Test
    void publish_approvedCourse_transitionsToPublished() {
        // given
        final Course course = createDraftCourse();
        course.approve();

        // when
        course.publish();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void sendToApprove_notSentForApproval_transitionsToWaitingForApproval() {
        // given
        final Course course = createDraftCourse();

        // when
        course.sendToApprove();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }
}
