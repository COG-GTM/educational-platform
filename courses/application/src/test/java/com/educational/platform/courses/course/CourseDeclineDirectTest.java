package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Course#decline()} directly sets the approval status to DECLINED.
 */
public class CourseDeclineDirectTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void decline_fromNotSentForApproval_setsStatusToDeclined() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void decline_fromWaitingForApproval_setsStatusToDeclined() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);
        course.sendToApprove();

        // when
        course.decline();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.DECLINED);
    }

    @Test
    void decline_preservesOtherFields() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("Course Name").description("Course Desc").build(), TEACHER_ID);

        // when
        course.decline();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("name", "Course Name")
                .hasFieldOrPropertyWithValue("description", "Course Desc")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT);
    }
}
