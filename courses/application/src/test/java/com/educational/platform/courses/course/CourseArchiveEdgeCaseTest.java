package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseArchiveEdgeCaseTest {

    private static final Integer TEACHER_ID = 15;

    @Test
    void archive_draftCourse_archivedWithoutApproval() {
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
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void archive_calledTwice_remainsArchived() {
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
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED);
    }

    @Test
    void archive_preservesApprovalStatus() {
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
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void archive_preservesRatingAndStudents() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, TEACHER_ID);
        course.updateRating(4.5);
        course.increaseNumberOfStudents();
        course.increaseNumberOfStudents();

        // when
        course.archive();

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.ARCHIVED)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5))
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }

    @Test
    void toIdentity_stableAcrossMultipleCalls() {
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
}
