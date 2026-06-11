package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CourseFullLifecycleTest {

    @Test
    void sendToApprove_fromDraft_transitionsToWaitingForApproval() {
        // given
        final Course course = createCourse();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_fromDeclined_transitionsToWaitingForApproval() {
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
    void sendToApprove_alreadyApproved_throwsCourseAlreadyApprovedException() {
        // given
        final Course course = createCourse();
        course.approve();

        // when / then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class)
                .isThrownBy(course::sendToApprove);
    }

    @Test
    void publish_approvedCourse_transitionsToPublished() {
        // given
        final Course course = createCourse();
        course.approve();

        // when
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void publish_notApprovedCourse_throwsCourseCannotBePublishedException() {
        // given
        final Course course = createCourse();

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(course::publish);
    }

    @Test
    void publish_declinedCourse_throwsCourseCannotBePublishedException() {
        // given
        final Course course = createCourse();
        course.decline();

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(course::publish);
    }

    @Test
    void publish_waitingForApprovalCourse_throwsCourseCannotBePublishedException() {
        // given
        final Course course = createCourse();
        course.sendToApprove();

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(course::publish);
    }

    @Test
    void archive_publishedCourse_transitionsToArchived() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_draftCourse_transitionsToArchived() {
        // given
        final Course course = createCourse();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void fullLifecycle_draftToApprovedToPublished() {
        // given
        final Course course = createCourse();

        // initial state
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT);

        // when: send to approve -> approve -> publish
        course.sendToApprove();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);

        course.approve();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);

        course.publish();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void increaseNumberOfStudents_calledMultipleTimes_incrementsCorrectly() {
        // given
        final Course course = createCourse();
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }

    @Test
    void updateRating_setsNewRating() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void updateRating_calledMultipleTimes_lastValueWins() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(3.0);
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void constructor_initializesWithUuidAndDefaults() {
        // when
        final Course course = createCourse();

        // then
        assertThat(course.toIdentity()).isNotNull();
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "desc")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    @Test
    void constructor_eachInstanceGetsUniqueUuid() {
        // when
        final Course course1 = createCourse();
        final Course course2 = createCourse();

        // then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

    @Test
    void courseAlreadyApprovedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseAlreadyApprovedException exception = new CourseAlreadyApprovedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("already approved");
    }

    @Test
    void courseCannotBePublishedException_messageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseCannotBePublishedException exception = new CourseCannotBePublishedException(uuid);

        // then
        assertThat(exception.getMessage())
                .contains(uuid.toString())
                .contains("cannot be published");
    }

    private Course createCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }
}
