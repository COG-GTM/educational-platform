package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link Course#archive()} domain method, verifying that archiving
 * works from every publish state.
 */
public class CourseArchiveTest {

    @Test
    void archive_fromDraft_transitionsToArchived() {
        // given
        final Course course = newCourse();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_fromPublished_transitionsToArchived() {
        // given
        final Course course = newCourse();
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_remainsArchived() {
        // given
        final Course course = newCourse();
        course.archive();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_preservesApprovalStatus() {
        // given
        final Course course = newCourse();
        course.approve();
        course.publish();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    private Course newCourse() {
        return new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
    }
}
