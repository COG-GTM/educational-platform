package com.educational.platform.courses.course.approve;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApproveCourseCommandHandlerEdgeCasesTest {

    @Mock
    private CourseRepository repository;

    private ApproveCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        sut = new ApproveCourseCommandHandler(repository);
    }

    @Test
    void handle_courseNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(new ApproveCourseCommand(uuid));

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(handle)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void handle_existingCourse_approvesAndSavesCourse() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new ApproveCourseCommand(uuid));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }

    @Test
    void handle_declinedCourse_approvesSuccessfully() {
        // given
        final UUID uuid = UUID.randomUUID();
        final Course course = new Course(
                CreateCourseCommand.builder().name("name").description("desc").build(), 1);
        course.decline();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(course));

        // when
        sut.handle(new ApproveCourseCommand(uuid));

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.APPROVED);
    }
}
