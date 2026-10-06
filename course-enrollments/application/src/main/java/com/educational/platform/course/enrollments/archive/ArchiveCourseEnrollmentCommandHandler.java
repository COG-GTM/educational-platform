package com.educational.platform.course.enrollments.archive;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;

import jakarta.annotation.Nonnull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for {@link ArchiveCourseEnrollmentCommand} archives the student's enrollment.
 */
@Component
@Transactional
public class ArchiveCourseEnrollmentCommandHandler {

    private final CourseEnrollmentRepository repository;
    private final CurrentUserAsStudent currentUserAsStudent;

    public ArchiveCourseEnrollmentCommandHandler(CourseEnrollmentRepository repository, CurrentUserAsStudent currentUserAsStudent) {
        this.repository = repository;
        this.currentUserAsStudent = currentUserAsStudent;
    }

    /**
     * Archives the enrollment.
     *
     * @param command command
     * @return enrollment after the change
     * @throws ResourceNotFoundException if the enrollment is not found
     */
    @Nonnull
    @PreAuthorize("hasRole('STUDENT')")
    public CourseEnrollmentDTO handle(ArchiveCourseEnrollmentCommand command) {
        final CourseEnrollment enrollment = repository.findByUuidAndStudent(command.uuid(), currentUserAsStudent.userAsStudent())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Course enrollment with uuid: %s not found", command.uuid())));

        enrollment.archive();
        repository.save(enrollment);

        return enrollment.toDTO();
    }
}
