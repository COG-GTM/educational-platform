package com.educational.platform.courses.course;


import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CourseTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void approve_approvedStatus() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.approve();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void decline_declinedStatus() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.decline();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void publish_approvedCourse_publishedStatus() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);
        final Course course = new Course(createCourseCommand, TEACHER_ID);
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
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        ReflectionTestUtils.setField(course, "id", 15);

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void create_validCommand_createdCourse() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_courseAlreadyApproved_courseAlreadyApprovedException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);
        final Course course = new Course(command, TEACHER_ID);
        ReflectionTestUtils.setField(course, "id", 15);
        ReflectionTestUtils.setField(course, "approvalStatus", ApprovalStatus.APPROVED);

        // when
        final ThrowableAssert.ThrowingCallable sendToApprove = course::sendToApprove;

        // then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class).isThrownBy(sendToApprove);
    }

    @Test
    void sendToApprove_waitingForApprovalStatus() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        var createTeacherCommand = new CreateTeacherCommand("username");
        var teacher = new Teacher(createTeacherCommand);
        final Course course = new Course(command, TEACHER_ID);
        ReflectionTestUtils.setField(course, "id", 15);

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void archive_archivedStatus() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void updateRating_ratingUpdated() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.updateRating(4.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void updateRating_zero_ratingUpdated() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.updateRating(3.0);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void increaseNumberOfStudents_numberOfStudentsIncremented() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void increaseNumberOfStudents_calledTwice_numberOfStudentsIncrementedTwice() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void toIdentity_returnsUuid() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        final java.util.UUID identity = course.toIdentity();

        // then
        assertThat(identity).isNotNull();
    }

    @Test
    void toIdentity_calledTwice_returnsSameUuid() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        final java.util.UUID first = course.toIdentity();
        final java.util.UUID second = course.toIdentity();

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    void create_initialRatingIsZero() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0));
    }

    @Test
    void create_initialNumberOfStudentsIsZero() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    @Test
    void create_uuidGenerated() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course.toIdentity()).isNotNull();
    }

    @Test
    void create_teacherIdStored() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("teacher", TEACHER_ID);
    }

    @Test
    void create_twoCourses_differentUuids() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course1 = new Course(command, TEACHER_ID);
        final Course course2 = new Course(command, TEACHER_ID);

        // then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

    @Test
    void sendToApprove_declinedCourse_waitingForApprovalStatus() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.decline();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void archive_publishedCourse_archivedStatus() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void create_nullCurriculumItems_noCurriculumItemsSet() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then — curriculumItems is null when command has no items
        assertThat(course)
                .hasFieldOrPropertyWithValue("curriculumItems", null);
    }

    @Test
    void publish_declinedCourse_courseCannotBePublishedException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.decline();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void publish_waitingForApprovalCourse_courseCannotBePublishedException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.sendToApprove();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void approve_declinedCourse_approvedStatus() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.decline();

        // when
        course.approve();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void approve_calledTwice_remainsApproved() {
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
    void updateRating_negativeValue_ratingUpdated() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.updateRating(-1.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(-1.5));
    }

    @Test
    void updateRating_calledMultipleTimes_lastValueWins() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.updateRating(1.0);
        course.updateRating(3.0);
        course.updateRating(5.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void create_implementsAggregateRoot() {
        // then
        assertThat(com.educational.platform.common.domain.AggregateRoot.class)
                .isAssignableFrom(Course.class);
    }

    @Test
    void create_initialPublishStatusIsDraft() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT);
    }

    @Test
    void create_initialApprovalStatusIsNotSent() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void sendToApprove_waitingForApprovalCourse_remainsWaiting() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.sendToApprove();

        // when
        course.sendToApprove();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void fullLifecycle_create_approve_publish_archive() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when — full happy path
        course.sendToApprove();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.WAITING_FOR_APPROVAL);

        course.approve();
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);

        course.publish();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);

        course.archive();
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void create_withCurriculumItems_curriculumItemsPopulated() {
        // given
        final var lecture = com.educational.platform.courses.course.create.CreateLectureCommand.builder()
                .title("Intro")
                .description("Introduction lecture")
                .serialNumber(1)
                .text("Content text")
                .build();
        final var question = new com.educational.platform.courses.course.create.CreateQuestionCommand("Q1");
        final var quiz = com.educational.platform.courses.course.create.CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("First quiz")
                .serialNumber(2)
                .text("Quiz text")
                .questions(java.util.List.of(question))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(java.util.List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .extracting("curriculumItems")
                .asList()
                .hasSize(2);
    }

}
