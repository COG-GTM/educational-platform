package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests archiving {@link Course} from various publish/approval states.
 * Verifies that archive() is always allowed regardless of current state.
 */
public class CourseArchiveFromDraftTest {

    private static final Integer TEACHER_ID = 1;

    @Test
    void archive_draftCourse_archivesSuccessfully() {
        // given
        final Course course = createCourse();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT);

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_draftCourse_approvalStatusUnchanged() {
        // given
        final Course course = createCourse();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void archive_alreadyArchived_remainsArchived() {
        // given
        final Course course = createCourse();
        course.archive();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void decline_fromNotSentForApproval_setsDeclined() {
        // given
        final Course course = createCourse();

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void decline_fromWaitingForApproval_setsDeclined() {
        // given
        final Course course = createCourse();
        course.sendToApprove();

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void approve_fromNotSentForApproval_setsApproved() {
        // given
        final Course course = createCourse();

        // when
        course.approve();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    private Course createCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
    }
}
