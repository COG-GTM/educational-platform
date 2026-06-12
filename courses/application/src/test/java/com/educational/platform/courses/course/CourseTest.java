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
    void archive_publishedCourse_archivedStatus() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void updateRating_validRating_ratingUpdated() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.updateRating(4.5);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void increaseNumberOfStudents_initialCourse_numberOfStudentsIncremented() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void increaseNumberOfStudents_calledTwice_numberOfStudentsIsTwo() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void toIdentity_createdCourse_uuidReturned() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        final java.util.UUID identity = course.toIdentity();

        // then
        assertThat(identity).isNotNull();
    }

    @Test
    void create_commandWithCurriculumItems_courseWithCurriculumItemsCreated() {
        // given
        final CreateLectureCommand lectureCommand = CreateLectureCommand.builder()
                .title("lecture-title")
                .description("lecture-description")
                .serialNumber(1)
                .text("lecture-content")
                .build();
        final CreateQuizCommand quizCommand = CreateQuizCommand.builder()
                .title("quiz-title")
                .description("quiz-description")
                .serialNumber(2)
                .text("quiz-text")
                .questions(List.of(new CreateQuestionCommand("q1")))
                .build();
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(lectureCommand, quizCommand))
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description");
        assertThat(course).extracting("curriculumItems").asList().hasSize(2);
    }

    @Test
    void create_nullCurriculumItems_courseCreatedWithoutItems() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(null)
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "name")
                .hasFieldOrPropertyWithValue("description", "description")
                .hasFieldOrPropertyWithValue("curriculumItems", null);
    }

    @Test
    void archive_draftCourse_archivedStatus() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
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
    void publish_declinedCourse_courseCannotBePublishedException() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        course.decline();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void publish_waitingForApprovalCourse_courseCannotBePublishedException() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        course.sendToApprove();

        // when
        final ThrowableAssert.ThrowingCallable publish = course::publish;

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(publish);
    }

    @Test
    void updateRating_zeroRating_ratingUpdated() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        course.updateRating(0.0);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void toIdentity_twoCoursesWithSameCommand_differentUuids() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course1 = new Course(command, TEACHER_ID);
        final Course course2 = new Course(command, TEACHER_ID);

        // when / then
        assertThat(course1.toIdentity()).isNotEqualTo(course2.toIdentity());
    }

}
