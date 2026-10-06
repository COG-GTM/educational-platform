package com.educational.platform.course.enrollments;

import com.educational.platform.common.exception.RelatedResourceIsNotResolvedException;
import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;
import com.educational.platform.course.enrollments.register.RegisterStudentToCourseCommand;
import com.educational.platform.course.enrollments.student.Student;
import com.educational.platform.course.enrollments.student.StudentRepository;
import com.educational.platform.course.enrollments.student.create.CreateStudentCommand;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;

/**
 * Represents course enrollment factory.
 */
@Component
public class CourseEnrollmentFactory {

    private final Validator validator;
    private final EnrollCourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserAsStudent currentUserAsStudent;

    public CourseEnrollmentFactory(Validator validator, EnrollCourseRepository courseRepository, StudentRepository studentRepository,
                                   CurrentUserAsStudent currentUserAsStudent) {
        this.validator = validator;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.currentUserAsStudent = currentUserAsStudent;
    }

    /**
     * Creates course enrollment from command.
     *
     * @param command command.
     * @return course enrollment.
     * @throws ConstraintViolationException          in the case of validation issues
     * @throws RelatedResourceIsNotResolvedException if student or course is not found by id
     */
    public CourseEnrollment createFrom(RegisterStudentToCourseCommand command) {
        final Set<ConstraintViolation<RegisterStudentToCourseCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        final EnrollCourse course = courseRepository.findByUuid(command.courseId())
                .orElseThrow(() -> new RelatedResourceIsNotResolvedException("Course cannot be found by uuid = " + command.courseId()));

        final Student student = Optional.ofNullable(currentUserAsStudent.userAsStudent())
                .orElseGet(() -> studentRepository.save(new Student(new CreateStudentCommand(currentUserAsStudent.username()))));

        return new CourseEnrollment(course, student);
    }
}
