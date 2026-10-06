package com.educational.platform.course.enrollments.progress;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentArchivedException;
import com.educational.platform.course.enrollments.CourseEnrollmentDetailsDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;

import jakarta.annotation.Nonnull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for {@link UpdateLectureProgressCommand} records the student's progress on a lecture.
 */
@Component
@Transactional
public class UpdateLectureProgressCommandHandler {

    private final CourseEnrollmentRepository repository;
    private final CurrentUserAsStudent currentUserAsStudent;

    public UpdateLectureProgressCommandHandler(CourseEnrollmentRepository repository, CurrentUserAsStudent currentUserAsStudent) {
        this.repository = repository;
        this.currentUserAsStudent = currentUserAsStudent;
    }

    /**
     * Marks the lecture as completed or not completed and returns the refreshed enrollment.
     *
     * @param command command
     * @return enrollment details after the change
     * @throws ResourceNotFoundException         if the enrollment or the lecture is not found
     * @throws CourseEnrollmentArchivedException if the enrollment is archived
     */
    @Nonnull
    @PreAuthorize("hasRole('STUDENT')")
    public CourseEnrollmentDetailsDTO handle(UpdateLectureProgressCommand command) {
        final CourseEnrollment enrollment = repository.findByUuidAndStudent(command.enrollmentUuid(), currentUserAsStudent.userAsStudent())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Course enrollment with uuid: %s not found", command.enrollmentUuid())));

        if (command.completed()) {
            enrollment.completeLecture(command.lectureUuid());
        } else {
            enrollment.resetLecture(command.lectureUuid());
        }
        repository.save(enrollment);

        return enrollment.toDetailsDTO();
    }
}
