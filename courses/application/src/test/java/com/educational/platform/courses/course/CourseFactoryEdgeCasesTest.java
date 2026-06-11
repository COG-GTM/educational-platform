package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.teacher.Teacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseFactoryEdgeCasesTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    @Mock
    private Teacher teacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_validCommand_createsWithDraftStatusAndTeacher() {
        // given
        when(currentUserAsTeacher.userAsTeacher()).thenReturn(teacher);
        when(teacher.getId()).thenReturn(42);
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("Math 101")
                .description("Introductory math course")
                .build();

        // when
        final Course result = sut.createFrom(command);

        // then
        assertThat(result.toIdentity()).isNotNull();
        assertThat(result)
                .hasFieldOrPropertyWithValue("name", "Math 101")
                .hasFieldOrPropertyWithValue("description", "Introductory math course")
                .hasFieldOrPropertyWithValue("publishStatus", PublishStatus.DRAFT)
                .hasFieldOrPropertyWithValue("approvalStatus", ApprovalStatus.NOT_SENT_FOR_APPROVAL)
                .hasFieldOrPropertyWithValue("teacher", 42);
    }

    @Test
    void createFrom_nullName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description("description")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_blankDescription_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_bothFieldsBlank_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("")
                .description("")
                .build();

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }
}
