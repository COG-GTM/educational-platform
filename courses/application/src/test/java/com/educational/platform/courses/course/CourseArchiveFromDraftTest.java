package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Course#archive()} can be invoked from any publish status,
 * including DRAFT (no guard prevents it).
 */
public class CourseArchiveFromDraftTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void archive_fromDraft_setsStatusToArchived() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_fromDraft_preservesApprovalStatus() {
        // given
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), TEACHER_ID);

        // when
        course.archive();

        // then
        assertThat(course).hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }
}
