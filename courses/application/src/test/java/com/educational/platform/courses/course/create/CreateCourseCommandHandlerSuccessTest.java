package com.educational.platform.courses.course.create;

import com.educational.platform.courses.course.*;
import com.educational.platform.courses.teacher.Teacher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCourseCommandHandlerSuccessTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private CourseRepository repository;

    private CreateCourseCommandHandler sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final CourseFactory courseFactory = new CourseFactory(validator, currentUserAsTeacher);
        sut = new CreateCourseCommandHandler(repository, courseFactory);
    }

    @Test
    void handle_validCommand_courseSavedWithCorrectName() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Java Basics")
                .description("Learn fundamentals of Java")
                .build();

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course savedCourse = argument.getValue();
        assertThat(savedCourse)
                .hasFieldOrPropertyWithValue("name", "Java Basics")
                .hasFieldOrPropertyWithValue("description", "Learn fundamentals of Java");
    }

    @Test
    void handle_validCommand_courseCreatedWithDraftStatus() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Spring Boot")
                .description("Advanced Spring Boot course")
                .build();

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course savedCourse = argument.getValue();
        assertThat(savedCourse)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void handle_validCommand_courseCreatedWithCorrectTeacher() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(42);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Kotlin")
                .description("Kotlin programming")
                .build();

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        final Course savedCourse = argument.getValue();
        assertThat(savedCourse)
                .hasFieldOrPropertyWithValue("teacher", 42);
    }

    @Test
    void handle_validCommand_returnedUuidMatchesSavedCourse() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(15);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Testing")
                .description("Unit testing best practices")
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        final ArgumentCaptor<Course> argument = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(argument.capture());
        assertThat(result).isEqualTo(argument.getValue().toIdentity());
    }
}
