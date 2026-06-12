package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CourseDomainStateTransitionTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void archive_draftCourse_archivedStatus() {
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
    void archive_waitingForApprovalCourse_archivedStatus() {
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
    void sendToApprove_declinedCourse_statusChangedToWaiting() {
        // given
        final Course course = createCourse();
        course.decline();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void approve_afterDecline_approvedStatus() {
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
    void publish_afterApproveAndArchive_canPublishAgain() {
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
    void updateRating_afterArchive_ratingUpdated() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();
        course.archive();

        // when
        course.updateRating(3.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3.5));
    }

    @Test
    void increaseNumberOfStudents_afterPublish_numberOfStudentsIncremented() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void toIdentity_multipleCallsSameCourse_returnsSameUuid() {
        // given
        final Course course = createCourse();

        // when
        final java.util.UUID first = course.toIdentity();
        final java.util.UUID second = course.toIdentity();

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void constructor_twoCourses_differentUuids() {
        // given / when
        final Course course1 = createCourse();
        final Course course2 = createCourse();

        // then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Test Course")
                .description("Test Description")
                .build();
        return new Course(command, TEACHER_ID);
    }
}
