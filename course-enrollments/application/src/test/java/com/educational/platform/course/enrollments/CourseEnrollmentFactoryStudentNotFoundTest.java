package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

/**
 * Tests the enrollment factory when the student resolution fails
 * and verifies a successful enrollment creates with expected course/student IDs.
 */
@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentFactoryStudentNotFoundTest {

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
    void createFrom_studentResolutionThrows_propagatesException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final EnrollCourse enrollCourse = new EnrollCourse(
                new com.educational.platform.course.enrollments.course.create.CreateCourseCommand(courseId));
        ReflectionTestUtils.setField(enrollCourse, "id", 10);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(enrollCourse));
        when(currentUserAsStudent.userAsStudent()).thenThrow(
                new RelatedResourceIsNotResolvedException("Student not found"));

        // when / then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(() -> sut.createFrom(command))
                .withMessageContaining("Student not found");
    }

    @Test
    void createFrom_validInputs_enrollmentHasCorrectCourseAndStudentIds() {
        // given
        final UUID courseId = UUID.randomUUID();
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        final EnrollCourse enrollCourse = new EnrollCourse(
                new com.educational.platform.course.enrollments.course.create.CreateCourseCommand(courseId));
        ReflectionTestUtils.setField(enrollCourse, "id", 10);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(enrollCourse));

        final com.educational.platform.course.enrollments.student.Student student =
                new com.educational.platform.course.enrollments.student.Student(
                        new com.educational.platform.course.enrollments.student.create.CreateStudentCommand("user"));
        ReflectionTestUtils.setField(student, "id", 20);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final CourseEnrollment result = sut.createFrom(command);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("course", 10)
                .hasFieldOrPropertyWithValue("student", 20)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
        assertThat(result.getUuid()).isNotNull();
    }
}
