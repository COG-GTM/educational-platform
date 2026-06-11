package com.educational.platform.course.enrollments;

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

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseEnrollmentFactorySuccessTest {

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
    void createFrom_validCommand_createsEnrollmentWithInProgressStatus() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(course));

        final Student student = new Student(new CreateStudentCommand("student1"));
        ReflectionTestUtils.setField(student, "id", 20);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        // when
        final CourseEnrollment result = sut.createFrom(command);

        // then
        assertThat(result.getUuid()).isNotNull();
        assertThat(result).hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
        assertThat(result).hasFieldOrPropertyWithValue("course", 10);
        assertThat(result).hasFieldOrPropertyWithValue("student", 20);
    }

    @Test
    void createFrom_validCommand_eachCallProducesUniqueUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final EnrollCourse course = new EnrollCourse(new CreateCourseCommand(courseId));
        ReflectionTestUtils.setField(course, "id", 10);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(course));

        final Student student = new Student(new CreateStudentCommand("student1"));
        ReflectionTestUtils.setField(student, "id", 20);
        when(currentUserAsStudent.userAsStudent()).thenReturn(student);

        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        // when
        final CourseEnrollment result1 = sut.createFrom(command);
        final CourseEnrollment result2 = sut.createFrom(command);

        // then
        assertThat(result1.getUuid()).isNotEqualTo(result2.getUuid());
    }
}
