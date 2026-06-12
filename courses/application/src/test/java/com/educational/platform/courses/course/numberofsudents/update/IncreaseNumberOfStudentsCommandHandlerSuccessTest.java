package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncreaseNumberOfStudentsCommandHandlerSuccessTest {

    private CourseFactory courseFactory;

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    @InjectMocks
    private IncreaseNumberOfStudentsCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        courseFactory = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void handle_existingCourse_numberOfStudentsIncremented() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

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
        final Course savedCourse = argument.getValue();
        assertThat(savedCourse)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void handle_calledTwice_numberOfStudentsIncrementedTwice() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

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
        sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).save(argument.capture());
        final Course lastSavedCourse = argument.getAllValues().get(1);
        assertThat(lastSavedCourse)
                .hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
    }
}
