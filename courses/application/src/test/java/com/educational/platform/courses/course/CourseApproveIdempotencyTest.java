package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that {@link Course#approve()} is idempotent and can be called
 * from any state without throwing.
 */
public class CourseApproveIdempotencyTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void approve_calledTwice_remainsApproved() {
        // given
        final Course course = newCourse();
        course.approve();

        // when
        course.approve();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void approve_fromDeclined_setsApproved() {
        // given
        final Course course = newCourse();
        course.decline();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);

        // when
        course.approve();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void approve_fromWaitingForApproval_setsApproved() {
        // given
        final Course course = newCourse();
        course.sendToApprove();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);

        // when
        course.approve();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void approve_fromNotSentForApproval_doesNotThrow() {
        // given
        final Course course = newCourse();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);

        // then
        assertThatCode(course::approve).doesNotThrowAnyException();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    private Course newCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
    }
}
