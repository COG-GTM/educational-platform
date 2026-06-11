package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests idempotent and repeated operations on {@link Course} that are not
 * guarded by exceptions: double-approve, double-publish, archive-then-publish
 * (re-publish), and negative rating updates.
 */
public class CourseIdempotentOperationsTest {

    private static final Integer TEACHER_ID = 1;

    private Course createApprovedCourse() {
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
        course.approve();
        return course;
    }

    @Test
    void approve_calledTwice_remainsApproved() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("n").description("d").build(), TEACHER_ID);

        // when
        course.approve();
        course.approve();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void publish_calledTwice_remainsPublished() {
        // given
        final Course course = createApprovedCourse();
        course.publish();

        // when – second publish should not throw
        assertThatNoException().isThrownBy(course::publish);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void archiveThenPublish_approvedCourse_rePublishesSuccessfully() {
        // given
        final Course course = createApprovedCourse();
        course.publish();
        course.archive();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);

        // when – re-publish after archive (approval still APPROVED)
        course.publish();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void updateRating_negativeValue_storedWithoutGuard() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("n").description("d").build(), TEACHER_ID);

        // when
        course.updateRating(-2.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(-2.5));
    }

    @Test
    void updateRating_fractionalValue_storedPrecisely() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("n").description("d").build(), TEACHER_ID);

        // when
        course.updateRating(3.14159);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(3.14159));
    }

    @Test
    void decline_thenSendToApprove_thenApprove_thenPublish_fullRecoveryLifecycle() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("n").description("d").build(), TEACHER_ID);
        course.sendToApprove();
        course.decline();

        // when – recover from decline
        course.sendToApprove();
        course.approve();
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }
}
