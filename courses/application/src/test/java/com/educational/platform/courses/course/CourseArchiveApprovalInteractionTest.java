package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests interactions between archive (publish status) and approval status transitions.
 * Archiving a course should not affect its approval status.
 */
public class CourseArchiveApprovalInteractionTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void archive_doesNotResetApprovalStatus() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();

        // when
        course.archive();

        // then — approval status remains APPROVED even after archiving
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void sendToApprove_afterArchiveFromDraft_transitionsToWaitingForApproval() {
        // given
        final Course course = createCourse();
        course.archive();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void publish_afterArchiveWithApproval_republishesSuccessfully() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();
        course.archive();

        // when — re-publish (approval is still APPROVED)
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void archive_fromWaitingForApproval_preservesApprovalStatus() {
        // given
        final Course course = createCourse();
        course.sendToApprove();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void archive_fromDeclined_preservesApprovalStatus() {
        // given
        final Course course = createCourse();
        course.decline();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    private Course createCourse() {
        return new Course(
                CreateCourseCommand.builder().name("Test Course").description("desc").build(),
                TEACHER_ID);
    }
}
