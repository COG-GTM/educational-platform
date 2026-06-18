package com.educational.platform.courses.course;


import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuestionCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

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
    void create_validCommand_zeroRatingAndZeroNumberOfStudents() {
        // given - a freshly created course is the baseline the enrollment and rating flows build on: zero students and zero rating
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(0));
    }

    @Test
    void sendToApprove_declinedCourse_waitingForApprovalStatus() {
        // given - the sendToApprove guard only blocks an already-approved course, so a declined course can be re-submitted for approval
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
    void increaseNumberOfStudents_freshCourse_numberOfStudentsIncrementedToOne() {
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
    void increaseNumberOfStudents_calledMultipleTimes_numberOfStudentsAccumulates() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(3));
    }

    @Test
    void updateRating_freshCourse_ratingSetToProvidedValue() {
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
    void updateRating_calledAgain_overwritesPreviousRating() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.updateRating(4.5);

        // when
        course.updateRating(2.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0));
    }

    @Test
    void updateRating_zeroValue_ratingSetToZero() {
        // given - boundary: a recalculation that resets the rating to zero (e.g. all reviews removed)
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.updateRating(4.5);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
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
    void publish_archivedApprovedCourse_republished() {
        // given - archiving is not terminal; an approved course can be published again after being archived
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.approve();
        course.publish();
        course.archive();

        // when
        course.publish();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }

    @Test
    void approve_alreadyApprovedCourse_remainsApproved() {
        // given - approve() has no guard, so re-delivery of the async CourseApprovedByAdmin event keeps the course approved
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.approve();

        // when
        course.approve();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void approve_declinedCourse_approvedStatus() {
        // given - approve() is unconditional, so a previously declined course can still be approved
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
    void decline_approvedCourse_declinedStatus() {
        // given - decline() is unconditional, so an already-approved course can still be moved to declined
        // (the symmetric counterpart of approve_declinedCourse_approvedStatus; relevant to a future
        // CourseDeclinedByAdmin consumer that would call Course.decline())
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.approve();

        // when
        course.decline();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void publish_declinedCourse_courseCannotBePublishedException() {
        // given - the publish guard rejects any non-approved status, including a declined course
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
    void create_withCurriculumItems_itemsMappedAndAttachedToCourse() {
        // given - when the command carries curriculum items the constructor maps each one onto the course
        final CreateLectureCommand lecture = CreateLectureCommand.builder()
                .title("Lecture 1")
                .description("lecture description")
                .serialNumber(1)
                .text("lecture body")
                .build();
        final CreateQuizCommand quiz = CreateQuizCommand.builder()
                .title("Quiz 1")
                .description("quiz description")
                .serialNumber(2)
                .text("quiz body")
                .questions(List.of(new CreateQuestionCommand("Q1")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(lecture, quiz))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat((List<?>) ReflectionTestUtils.getField(course, "curriculumItems"))
                .hasSize(2)
                .extracting("title")
                .containsExactly("Lecture 1", "Quiz 1");
    }

    @Test
    void create_withoutCurriculumItems_curriculumItemsNotInitialised() {
        // given - the constructor only maps curriculum items when the command supplies them
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(ReflectionTestUtils.getField(course, "curriculumItems")).isNull();
    }

    @Test
    void toIdentity_returnsCourseUuid() {
        // given - the course uuid is the natural key every cross-module event consumer uses to look the course up (repository.findByUuid)
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440005");
        ReflectionTestUtils.setField(course, "uuid", uuid);

        // when
        final UUID identity = course.toIdentity();

        // then
        assertThat(identity).isEqualTo(uuid);
    }

    @Test
    void sendToApprove_courseAlreadyApproved_exceptionMessageIdentifiesCourse() {
        // given - the moderation guard's message embeds the offending course uuid so the failure is diagnosable from logs
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440010");
        ReflectionTestUtils.setField(course, "uuid", uuid);
        ReflectionTestUtils.setField(course, "approvalStatus", ApprovalStatus.APPROVED);

        // when
        final ThrowableAssert.ThrowingCallable sendToApprove = course::sendToApprove;

        // then
        assertThatExceptionOfType(CourseAlreadyApprovedException.class)
                .isThrownBy(sendToApprove)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void publish_notApprovedCourse_exceptionMessageIdentifiesCourse() {
        // given - the publish guard rejection carries the course uuid in its message
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440011");
        ReflectionTestUtils.setField(course, "uuid", uuid);

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(publish)
                .withMessageContaining(uuid.toString());
    }

}
