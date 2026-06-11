package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CourseDomainMethodsTest {

    private static final Integer TEACHER_ID = 1;

    private Course createDraftCourse() {
        return new Course(CreateCourseCommand.builder().name("n").description("d").build(), TEACHER_ID);
    }

    @Test
    void updateRating_setsNewRatingValue() {
        // given
        final Course course = createDraftCourse();

        // when
        course.updateRating(4.5);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void updateRating_zeroRating_setsToZero() {
        // given
        final Course course = createDraftCourse();
        course.updateRating(3.0);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void increaseNumberOfStudents_incrementsByOne() {
        // given
        final Course course = createDraftCourse();
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
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
    void sendToApprove_alreadyApproved_throwsCourseAlreadyApprovedException() {
        // given
        final Course course = createDraftCourse();
        course.approve();

        // when
        final ThrowableAssert.ThrowingCallable action = course::sendToApprove;

        // then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class).isThrownBy(action);
    }

    @Test
    void publish_draftNotApproved_courseCannotBePublishedException() {
        // given
        final Course course = createDraftCourse();

        // when
        final ThrowableAssert.ThrowingCallable action = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(action);
    }

    @Test
    void archive_draftCourse_archivesSuccessfully() {
        // given
        final Course course = createDraftCourse();

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void toIdentity_returnsUuid() {
        // given
        final Course course = createDraftCourse();

        // then
        assertThat(course.toIdentity()).isNotNull();
    }
}
