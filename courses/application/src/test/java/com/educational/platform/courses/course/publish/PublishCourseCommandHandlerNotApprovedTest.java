package com.educational.platform.courses.course.publish;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

/**
 * Verifies that publishing a course that is NOT approved throws {@link CourseCannotBePublishedException}.
 */
@ExtendWith(MockitoExtension.class)
public class PublishCourseCommandHandlerNotApprovedTest {

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private PublishCourseCommandHandler sut;

    @Test
    void handle_courseNotApproved_throwsCourseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(() -> sut.handle(new PublishCourseCommand(uuid)))
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(ex.getMessage())
                        .contains("cannot be published"));
    }

    @Test
    void handle_courseDeclined_throwsCourseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        course.decline();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(() -> sut.handle(new PublishCourseCommand(uuid)));
    }

    @Test
    void handle_courseWaitingForApproval_throwsCourseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        course.sendToApprove();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when / then
        assertThatExceptionOfType(CourseCannotBePublishedException.class)
                .isThrownBy(() -> sut.handle(new PublishCourseCommand(uuid)));
    }
}
