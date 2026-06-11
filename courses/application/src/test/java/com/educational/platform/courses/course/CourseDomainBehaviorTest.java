package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests domain-level behaviors of {@link Course} that are not covered
 * by existing state-transition or lifecycle tests: updateRating boundaries,
 * sequential increaseNumberOfStudents, decline from various states, and archive
 * from draft.
 */
public class CourseDomainBehaviorTest {

    private static final Integer TEACHER_ID = 1;

    private Course createDraftCourse() {
        return new Course(CreateCourseCommand.builder()
                .name("name").description("description").build(), TEACHER_ID);
    }

    @Test
    void updateRating_zeroValue_setsRatingToZero() {
        // given
        final Course course = createDraftCourse();

        // when
        course.updateRating(0.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void updateRating_maxBoundaryValue_setsRating() {
        // given
        final Course course = createDraftCourse();

        // when
        course.updateRating(5.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void updateRating_overwritesPreviousRating() {
        // given
        final Course course = createDraftCourse();
        course.updateRating(3.0);

        // when
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void increaseNumberOfStudents_calledTwice_incrementsToTwo() {
        // given
        final Course course = createDraftCourse();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void decline_fromWaitingForApproval_setsDeclined() {
        // given
        final Course course = createDraftCourse();
        course.sendToApprove();

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void decline_fromNotSentForApproval_setsDeclined() {
        // given
        final Course course = createDraftCourse();

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void archive_draftCourse_setsArchived() {
        // given
        final Course course = createDraftCourse();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_calledTwice_remainsArchived() {
        // given
        final Course course = createDraftCourse();
        course.archive();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }
}
