package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentDetailsDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Query handler for {@link CourseEnrollmentByUUIDQuery} retrieves the current student's enrollment with its curriculum progress.
 */
@Component
@Transactional(readOnly = true)
public class CourseEnrollmentByUUIDQueryHandler {

    private final CourseEnrollmentRepository repository;
    private final CurrentUserAsStudent currentUserAsStudent;

    public CourseEnrollmentByUUIDQueryHandler(CourseEnrollmentRepository repository, CurrentUserAsStudent currentUserAsStudent) {
        this.repository = repository;
        this.currentUserAsStudent = currentUserAsStudent;
    }

    /**
     * Retrieves course enrollment by uuid.
     *
     * @param query query
     * @return course enrollment details or {@literal Optional#empty()} if the student has no such enrollment
     */
    @PreAuthorize("hasRole('STUDENT')")
    public Optional<CourseEnrollmentDetailsDTO> handle(CourseEnrollmentByUUIDQuery query) {
        return repository.findByUuidAndStudent(query.uuid(), currentUserAsStudent.userAsStudent())
                .map(CourseEnrollment::toDetailsDTO);
    }
}
