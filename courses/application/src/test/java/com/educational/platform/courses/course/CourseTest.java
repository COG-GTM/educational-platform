package com.educational.platform.courses.course;


import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.create.CreateLectureCommand;
import com.educational.platform.courses.course.create.CreateQuizCommand;
import com.educational.platform.courses.integration.event.CoursePublishedIntegrationEvent;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.LOCAL_DATE_TIME;

import java.time.LocalDateTime;
import java.util.List;

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
    void publish_approvedCourse_publishedDateSetToNow() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        course.approve();
        final LocalDateTime before = LocalDateTime.now();

        // when
        course.publish();

        // then
        final LocalDateTime after = LocalDateTime.now();
        assertThat(course).extracting("publishedDate", LOCAL_DATE_TIME)
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    void publish_notApprovedCourse_publishedDateStaysNull() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        ReflectionTestUtils.setField(course, "id", 15);

        // when
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(course::publish);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("publishedDate", null);
    }

    @Test
    void publish_republishedAfterArchive_firstPublishedDateKept() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);
        course.approve();
        course.publish();
        final LocalDateTime firstPublishedDate = (LocalDateTime) ReflectionTestUtils.getField(course, "publishedDate");
        course.archive();

        // when
        course.publish();

        // then
        assertThat(firstPublishedDate).isNotNull();
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED)
                .hasFieldOrPropertyWithValue("publishedDate", firstPublishedDate);
    }

    @Test
    void create_validCommand_publishedDateNull() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();

        // when
        final Course course = new Course(command, TEACHER_ID);

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishedDate", null);
    }

    @Test
    void toPublishedEvent_lecturesAndQuizzes_onlyLecturesInPublishedOrder() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .curriculumItems(List.of(
                        CreateLectureCommand.builder().title("Intro").serialNumber(1).text("text").build(),
                        CreateQuizCommand.builder().title("Quiz").serialNumber(2).text("text").questions(List.of()).build(),
                        CreateLectureCommand.builder().title("Basics").serialNumber(3).text("text").build()))
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        final CoursePublishedIntegrationEvent event = course.toPublishedEvent();

        // then
        assertThat(event.courseId()).isEqualTo(course.toIdentity());
        assertThat(event.name()).isEqualTo("name");
        assertThat(event.lectures()).extracting("title", "serialNumber")
                .containsExactly(tuple("Intro", 1), tuple("Basics", 3));
        assertThat(event.lectures()).extracting("uuid").doesNotContainNull().doesNotHaveDuplicates();
    }

    @Test
    void toPublishedEvent_noCurriculum_emptyLectures() {
        // given
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(createCourseCommand, TEACHER_ID);

        // when
        final CoursePublishedIntegrationEvent event = course.toPublishedEvent();

        // then
        assertThat(event.courseId()).isEqualTo(course.toIdentity());
        assertThat(event.name()).isEqualTo("name");
        assertThat(event.lectures()).isEmpty();
    }
}
