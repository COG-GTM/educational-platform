package com.educational.platform.courses.course.publish;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishCourseCommandHandlerEdgeCaseTest {

    private CourseFactory courseFactory;

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private PublishCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseFactory = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void handle_courseNotApproved_courseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final PublishCourseCommand command = new PublishCourseCommand(uuid);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course correspondingCourse = courseFactory.createFrom(createCourseCommand);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourse));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseDeclined_courseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final PublishCourseCommand command = new PublishCourseCommand(uuid);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course correspondingCourse = courseFactory.createFrom(createCourseCommand);
        correspondingCourse.decline();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourse));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseWaitingForApproval_courseCannotBePublishedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final PublishCourseCommand command = new PublishCourseCommand(uuid);

        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course correspondingCourse = courseFactory.createFrom(createCourseCommand);
        correspondingCourse.sendToApprove();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourse));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseCannotBePublishedException.class).isThrownBy(handle);
    }

    @Test
    void handle_courseNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final PublishCourseCommand command = new PublishCourseCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }
}
