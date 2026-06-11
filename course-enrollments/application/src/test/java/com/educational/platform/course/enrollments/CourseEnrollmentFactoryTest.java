package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.course.create.CreateCourseCommand;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentFactoryTest {

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
    void createFrom_validCommand_returnsEnrollmentWithCourseAndStudent() {
        // given
        final UUID courseId = UUID.randomUUID();
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(course));

        final Student student = new Student(new CreateStudentCommand("username"));
        ReflectionTestUtils.setField(student, "id", 20);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment).isNotNull();
        assertThat(enrollment.getUuid()).isNotNull();
        assertThat(enrollment).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
        assertThat(enrollment).hasFieldOrPropertyWithValue("course", 10);
        assertThat(enrollment).hasFieldOrPropertyWithValue("student", 20);
    }

    @Test
    void createFrom_courseIdIsNull_constraintViolationException() {
        // given
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(null);

        // when / then
        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> sut.createFrom(command));
    }

    @Test
    void createFrom_courseNotFound_relatedResourceIsNotResolvedException() {
        // given
        final UUID courseId = UUID.randomUUID();
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(RelatedResourceIsNotResolvedException.class)
                .isThrownBy(() -> sut.createFrom(command))
                .withMessageContaining(courseId.toString());
    }
}
