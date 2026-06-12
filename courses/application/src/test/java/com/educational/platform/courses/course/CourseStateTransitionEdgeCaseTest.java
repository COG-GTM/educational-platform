package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CourseStateTransitionEdgeCaseTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void sendToApprove_waitingForApprovalCourse_remainsWaitingForApproval() {
        // given
        final Course course = createCourse();
        course.sendToApprove();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_declinedThenResent_waitingForApproval() {
        // given
        final Course course = createCourse();
        course.sendToApprove();
        course.decline();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void publish_archivedApprovedCourse_publishStatusPublished() {
        // given
        final Course course = createCourse();
        course.approve();
        course.archive();

        // when
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void archive_archivedCourse_remainsArchived() {
        // given
        final Course course = createCourse();
        course.archive();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void approve_declinedCourse_statusChangesToApproved() {
        // given
        final Course course = createCourse();
        course.decline();

        // when
        course.approve();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void approve_approvedCourse_remainsApproved() {
        // given
        final Course course = createCourse();
        course.approve();

        // when
        course.approve();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void publish_approvedAndPublished_remainsPublished() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();

        // when
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(command, TEACHER_ID);
    }
}
