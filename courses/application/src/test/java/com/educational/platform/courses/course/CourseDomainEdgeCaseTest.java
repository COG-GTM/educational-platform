package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseDomainEdgeCaseTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void updateRating_zeroValue_ratingSetToZero() {
        // given
        final Course course = createCourse();
        course.updateRating(4.5);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0));
    }

    @Test
    void updateRating_maxValue_ratingSetToMax() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(5.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void updateRating_calledMultipleTimes_lastValueWins() {
        // given
        final Course course = createCourse();

        // when
        course.updateRating(1.0);
        course.updateRating(3.0);
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void increaseNumberOfStudents_calledMultipleTimes_incrementsCorrectly() {
        // given
        final Course course = createCourse();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }

    @Test
    void toIdentity_calledMultipleTimes_returnsSameValue() {
        // given
        final Course course = createCourse();

        // when
        final var uuid1 = course.toIdentity();
        final var uuid2 = course.toIdentity();

        // then
        assertThat(uuid1).isEqualTo(uuid2);
    }

    @Test
    void constructor_setsInitialDefaults() {
        // when
        final Course course = createCourse();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0))
                .hasFieldOrPropertyWithValue("teacher", TEACHER_ID);
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void archive_draftCourse_publishStatusChangedToArchived() {
        // given
        final Course course = createCourse();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_publishedCourse_publishStatusChangedToArchived() {
        // given
        final Course course = createCourse();
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void decline_afterSendToApprove_approvalStatusDeclined() {
        // given
        final Course course = createCourse();
        course.sendToApprove();

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void approve_afterDecline_approvalStatusApproved() {
        // given
        final Course course = createCourse();
        course.decline();

        // when
        course.approve();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Test Course")
                .description("Test Description")
                .build();
        return new Course(command, TEACHER_ID);
    }
}
