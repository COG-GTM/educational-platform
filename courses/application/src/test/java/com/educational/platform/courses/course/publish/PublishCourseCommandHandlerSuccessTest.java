package com.educational.platform.courses.course.publish;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the publish handler happy path: an approved course is saved with PUBLISHED status.
 */
@ExtendWith(MockitoExtension.class)
public class PublishCourseCommandHandlerSuccessTest {

    @Mock
    private CourseRepository repository;

    private PublishCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new PublishCourseCommandHandler(repository);
    }

    @Test
    void handle_approvedCourse_savesWithPublishedStatus() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        course.approve();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new PublishCourseCommand(uuid));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED);
    }
}
