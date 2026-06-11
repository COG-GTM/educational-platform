package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the {@link Course#sendToApprove()} domain method including the
 * guard clause that prevents re-sending an already-approved course.
 */
public class CourseSendToApproveTest {

    @Test
    void sendToApprove_fromDraft_transitionsToWaitingForApproval() {
        // given
        final Course course = newCourse();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_fromDeclined_transitionsToWaitingForApproval() {
        // given
        final Course course = newCourse();
        course.decline();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_alreadyApproved_throwsCourseAlreadyApprovedException() {
        // given
        final Course course = newCourse();
        course.approve();

        // when / then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class)
                .isThrownBy(course::sendToApprove);
    }

    @Test
    void sendToApprove_fromWaitingForApproval_remainsWaitingForApproval() {
        // given
        final Course course = newCourse();
        course.sendToApprove();

        // when — re-send (no guard blocks this)
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    private Course newCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }
}
