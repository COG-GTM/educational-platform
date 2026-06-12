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
class CourseEnrollmentFactoryEdgeCaseTest {

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
    void createFrom_validCommand_enrollmentHasCorrectCourseAndStudentReferences() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        ReflectionTestUtils.setField(correspondingCourse, "id", 42);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("student-username");
        final Student correspondingStudent = new Student(createStudentCommand);
        ReflectionTestUtils.setField(correspondingStudent, "id", 99);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment = sut.createFrom(command);

        // then
        assertThat(enrollment)
                .hasFieldOrPropertyWithValue("course", 42)
                .hasFieldOrPropertyWithValue("student", 99)
                .hasFieldOrPropertyWithValue("completionStatus", CompletionStatus.IN_PROGRESS);
        assertThat(enrollment.getUuid()).isNotNull();
    }

    @Test
    void createFrom_twoCalls_differentUuids() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final RegisterStudentToCourseCommand command = new RegisterStudentToCourseCommand(courseId);

        final CreateCourseCommand createCourseCommand = new CreateCourseCommand(courseId);
        final EnrollCourse correspondingCourse = new EnrollCourse(createCourseCommand);
        ReflectionTestUtils.setField(correspondingCourse, "id", 42);
        when(courseRepository.findByUuid(courseId)).thenReturn(Optional.of(correspondingCourse));

        final CreateStudentCommand createStudentCommand = new CreateStudentCommand("student-username");
        final Student correspondingStudent = new Student(createStudentCommand);
        ReflectionTestUtils.setField(correspondingStudent, "id", 99);
        when(currentUserAsStudent.userAsStudent()).thenReturn(correspondingStudent);

        // when
        final CourseEnrollment enrollment1 = sut.createFrom(command);
        final CourseEnrollment enrollment2 = sut.createFrom(command);

        // then
        assertThat(enrollment1.getUuid()).isNotEqualTo(enrollment2.getUuid());
    }
}
