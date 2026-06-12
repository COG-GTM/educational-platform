package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CourseStateTransitionTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void sendToApprove_notSentForApproval_statusIsWaitingForApproval() {
        // given
        final Course course = createCourse();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_alreadyApproved_courseAlreadyApprovedException() {
        // given
        final Course course = createCourse();
        course.approve();

        // when
        final ThrowableAssert.ThrowingCallable sendToApprove = course::sendToApprove;

        // then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class).isThrownBy(sendToApprove);
    }

    @Test
    void sendToApprove_declinedCourse_statusIsWaitingForApproval() {
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
    void publish_approvedCourse_statusIsPublished() {
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
    void publish_notApprovedCourse_courseCannotBePublishedException() {
        // given
        final Course course = createCourse();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void publish_waitingForApprovalCourse_courseCannotBePublishedException() {
        // given
        final Course course = createCourse();
        course.sendToApprove();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void archive_publishedCourse_statusIsArchived() {
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
    void archive_draftCourse_statusIsArchived() {
        // given
        final Course course = createCourse();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void constructor_withCurriculumItems_itemsCreated() {
        // given
        final CreateLectureCommand lecture1 = CreateLectureCommand.builder()
                .title("Introduction")
                .description("First lecture")
                .serialNumber(1)
                .text("Welcome to the course")
                .build();
        final CreateLectureCommand lecture2 = CreateLectureCommand.builder()
                .title("Chapter 1")
                .description("Second lecture")
                .serialNumber(2)
                .text("Core concepts")
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Java Basics")
                .description("Learn Java")
                .curriculumItems(List.of(lecture1, lecture2))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Java Basics")
                .hasFieldOrPropertyWithValue("description", "Learn Java")
                .hasFieldOrPropertyWithValue("teacher", TEACHER_ID);
        assertThat(course).extracting("curriculumItems")
                .satisfies(items -> assertThat((List<?>) items).hasSize(2));
    }

    @Test
    void constructor_withNullCurriculumItems_noException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Course")
                .description("Description")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Course")
                .hasFieldOrPropertyWithValue("curriculumItems", null);
    }

    @Test
    void constructor_initialState_draftAndNotSentForApproval() {
        // given / when
        final Course course = createCourse();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    private Course createCourse() {
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Test Course")
                .description("Description")
                .build();
        return new Course(command, TEACHER_ID);
    }
}
