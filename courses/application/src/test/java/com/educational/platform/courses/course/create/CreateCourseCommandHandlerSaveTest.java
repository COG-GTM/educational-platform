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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateCourseCommandHandlerSaveTest {

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
    void handle_validCommand_savesCourseWithCorrectFields() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(42);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Advanced Math")
                .description("Calculus and beyond")
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        assertThat(result).isNotNull();
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue())
                .hasFieldOrPropertyWithValue("name", "Advanced Math")
                .hasFieldOrPropertyWithValue("description", "Calculus and beyond")
                .hasFieldOrPropertyWithValue("teacher", 42)
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL);
    }

    @Test
    void handle_validCommand_returnedUuidMatchesSavedCourse() {
        // given
        var teacher = mock(Teacher.class);
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(1);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Physics")
                .description("Mechanics")
                .build();

        // when
        final UUID result = sut.handle(command);

        // then
        final ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().toIdentity()).isEqualTo(result);
    }
}
