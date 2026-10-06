package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query handler for {@link CourseEnrollmentByCourseQuery} tells whether (and how) the current student is enrolled to the course.
 */
@Component
@Transactional(readOnly = true)
public class CourseEnrollmentByCourseQueryHandler {

    private final CourseEnrollmentRepository repository;
    private final EnrollCourseRepository courseRepository;
    private final CurrentUserAsStudent currentUserAsStudent;

    public CourseEnrollmentByCourseQueryHandler(CourseEnrollmentRepository repository, EnrollCourseRepository courseRepository, CurrentUserAsStudent currentUserAsStudent) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.currentUserAsStudent = currentUserAsStudent;
    }

    /**
     * Retrieves the current student's enrollment to the course.
     *
     * @param query query
     * @return course enrollment or {@literal Optional#empty()} if the student is not enrolled
     */
    @PreAuthorize("hasRole('STUDENT')")
    public Optional<CourseEnrollmentDTO> handle(CourseEnrollmentByCourseQuery query) {
        return courseRepository.findByUuid(query.courseUuid())
                .flatMap(course -> repository.findByCourseAndStudent(course, currentUserAsStudent.userAsStudent()))
                .map(CourseEnrollment::toDTO);
    }
}
