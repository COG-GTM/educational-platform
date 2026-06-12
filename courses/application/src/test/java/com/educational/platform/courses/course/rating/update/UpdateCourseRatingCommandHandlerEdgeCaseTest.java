package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCourseRatingCommandHandlerEdgeCaseTest {

    private CourseFactory courseFactory;

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private UpdateCourseRatingCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseFactory = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void handle_zeroRating_courseSavedWithZeroRating() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 0.0);

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
        sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course course = argument.getValue();
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void handle_maxRating_courseSavedWithMaxRating() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 5.0);

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
        sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course course = argument.getValue();
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void handle_consecutiveUpdates_lastRatingWins() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

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
        sut.handle(new UpdateCourseRatingCommand(uuid, 3.0));
        sut.handle(new UpdateCourseRatingCommand(uuid, 4.5));

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository, org.mockito.Mockito.times(2)).save(argument.capture());
        final Course course = argument.getValue();
        assertThat(course)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
    }

    @Test
    void handle_courseNotFound_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 4.0);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }
}
