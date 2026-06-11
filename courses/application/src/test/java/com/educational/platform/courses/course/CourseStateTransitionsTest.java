package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CourseStateTransitionsTest {

    private static final Integer TEACHER_ID = 15;

    private Course createDraftCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(command, TEACHER_ID);
    }

    @Test
    void publish_waitingForApproval_courseCannotBePublishedException() {
        // given
        final Course course = createDraftCourse();
        ReflectionTestUtils.setField(course, "approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void publish_declined_courseCannotBePublishedException() {
        // given
        final Course course = createDraftCourse();
        course.decline();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void sendToApprove_fromDraft_transitionsToWaitingForApproval() {
        // given
        final Course course = createDraftCourse();

        // when
        course.sendToApprove();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_fromDeclined_transitionsToWaitingForApproval() {
        // given
        final Course course = createDraftCourse();
        course.decline();

        // when
        course.sendToApprove();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_fromWaitingForApproval_transitionsToWaitingForApproval() {
        // given
        final Course course = createDraftCourse();
        course.sendToApprove();

        // when
        course.sendToApprove();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void fullLifecycle_draft_sendToApprove_approve_publish() {
        // given
        final Course course = createDraftCourse();

        // when
        course.sendToApprove();
        course.approve();
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void fullLifecycle_decline_resend_approve_publish() {
        // given
        final Course course = createDraftCourse();

        // when
        course.sendToApprove();
        course.decline();
        course.sendToApprove();
        course.approve();
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void archive_publishedCourse_archivesSuccessfully() {
        // given
        final Course course = createDraftCourse();
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }
}
