package com.educational.platform.courses.course;

import com.educational.platform.courses.course.create.CreateCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class CourseFactoryValidationTest {

    @Mock
    private CurrentUserAsTeacher currentUserAsTeacher;

    private CourseFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseFactory(validator, currentUserAsTeacher);
    }

    @Test
    void createFrom_nullName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description("description")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }

    @Test
    void createFrom_blankName_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("   ")
                .description("description")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }

    @Test
    void createFrom_nullDescription_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description(null)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }

    @Test
    void createFrom_blankDescription_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("")
                .build();

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }

    @Test
    void createFrom_bothFieldsNull_constraintViolationException() {
        // given
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name(null)
                .description(null)
                .build();

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }
}
