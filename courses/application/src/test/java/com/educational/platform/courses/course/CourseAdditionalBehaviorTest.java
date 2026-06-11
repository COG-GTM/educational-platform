package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseAdditionalBehaviorTest {

    private static final Integer TEACHER_ID = 15;

    private Course createDraftCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("description").build(),
                TEACHER_ID);
    }

    @Test
    void toIdentity_returnsNonNullUuid() {
        // given
        final Course course = createDraftCourse();

        // when / then
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void toIdentity_eachCourseHasDistinctUuid() {
        // given
        final Course first = createDraftCourse();
        final Course second = createDraftCourse();

        // when / then
        assertThat(first.toIdentity()).isNotEqualTo(second.toIdentity());
    }

    @Test
    void archive_draftCourse_setsArchivedStatus() {
        // given
        final Course course = createDraftCourse();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchivedCourse_remainsArchived() {
        // given
        final Course course = createDraftCourse();
        course.archive();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void updateRating_setsNewRatingValue() {
        // given
        final Course course = createDraftCourse();

        // when
        course.updateRating(3.7);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(3.7));
    }

    @Test
    void updateRating_zeroRating_resetsToZero() {
        // given
        final Course course = createDraftCourse();
        course.updateRating(4.0);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void increaseNumberOfStudents_twiceFromZero_resultIsTwo() {
        // given
        final Course course = createDraftCourse();

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void initialState_ratingIsZero() {
        // given
        final Course course = createDraftCourse();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0));
    }

    @Test
    void initialState_numberOfStudentsIsZero() {
        // given
        final Course course = createDraftCourse();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }
}
