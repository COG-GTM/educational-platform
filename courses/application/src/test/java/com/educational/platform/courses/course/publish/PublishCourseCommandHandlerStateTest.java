package com.educational.platform.courses.course.publish;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PublishCourseCommandHandlerStateTest {

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
        ReflectionTestUtils.setField(course, "approvalStatus", ApprovalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new PublishCourseCommand(uuid));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.PUBLISHED)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void handle_approvedCourse_preservesNameAndDescription() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("My Course").description("A great course").build(), 42);
        ReflectionTestUtils.setField(course, "approvalStatus", ApprovalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new PublishCourseCommand(uuid));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("name", "My Course")
                .hasFieldOrPropertyWithValue("description", "A great course")
                .hasFieldOrPropertyWithValue("teacher", 42);
    }
}
