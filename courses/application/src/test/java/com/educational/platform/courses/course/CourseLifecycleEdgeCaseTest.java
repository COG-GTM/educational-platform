package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CourseLifecycleEdgeCaseTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void fullLifecycle_createApprovePublishArchive_stateTransitionsCorrect() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Full Lifecycle Course")
                .description("Testing full lifecycle")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when / then - each step in the lifecycle
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT);

        course.approve();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);

        course.publish();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);

        course.archive();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void decline_thenDeclineAgain_noExceptionThrown() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.decline();
        course.decline();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void approve_thenApproveAgain_noExceptionThrown() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.approve();
        course.approve();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void publish_notApproved_courseCannotBePublishedException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void sendToApprove_thenDecline_thenResendToApprove_stateTransitionsCorrect() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.sendToApprove();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);

        course.decline();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);

        course.sendToApprove();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void updateRating_afterPublish_ratingUpdated() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.approve();
        course.publish();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5))
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void increaseNumberOfStudents_afterPublish_numberOfStudentsIncremented() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.approve();
        course.publish();

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1))
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }
}
