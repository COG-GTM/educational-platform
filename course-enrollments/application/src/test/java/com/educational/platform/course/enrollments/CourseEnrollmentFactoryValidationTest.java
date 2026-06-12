package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentFactoryValidationTest {

    @Mock
    private EnrollCourseRepository courseRepository;

    @Mock
    private CurrentUserAsStudent currentUserAsStudent;

    private CourseEnrollmentFactory sut;

    @BeforeEach
    void setUp() {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        sut = new CourseEnrollmentFactory(validator, courseRepository, currentUserAsStudent);
    }

    @Test
    void createFrom_courseNotFound_relatedResourceIsNotResolvedException() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class).isThrownBy(createAction);
    }

    @Test
    void createFrom_nullCourseId_constraintViolationException() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(null);

        // when
        final ThrowableAssert.ThrowingCallable createAction = () -> sut.createFrom(command);

        // then
        assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(createAction);
    }
}
