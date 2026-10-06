package com.educational.platform.course.enrollments.query;

import com.educational.platform.course.enrollments.CompletionStatus;
import com.educational.platform.course.enrollments.CourseEnrollment;
import com.educational.platform.course.enrollments.CourseEnrollmentCountsDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentPageDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRepository;
import com.educational.platform.course.enrollments.CurrentUserAsStudent;
import com.educational.platform.course.enrollments.student.Student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query handler for {@link ListCourseEnrollmentsQuery} retrieves the current student's enrollments, most recently
 * active first, optionally narrowed to one dashboard group.
 */
@Component
@Transactional(readOnly = true)
public class ListCourseEnrollmentsQueryHandler {

    private static final Sort MOST_RECENT_ACTIVITY = Sort.by(Sort.Order.desc("lastActivityAt"), Sort.Order.desc("id"));

    private final CourseEnrollmentRepository repository;
    private final CurrentUserAsStudent currentUserAsStudent;

    public ListCourseEnrollmentsQueryHandler(CourseEnrollmentRepository repository, CurrentUserAsStudent currentUserAsStudent) {
        this.repository = repository;
        this.currentUserAsStudent = currentUserAsStudent;
    }

    /**
     * Retrieves a page of course enrollments.
     *
     * @param query query
     * @return page of course enrollments
     */
    @PreAuthorize("hasRole('STUDENT')")
    public CourseEnrollmentPageDTO handle(ListCourseEnrollmentsQuery query) {
        final Student student = currentUserAsStudent.userAsStudent();
        if (student == null) {
            return new CourseEnrollmentPageDTO(List.of(), query.page(), query.size(), 0, 0, new CourseEnrollmentCountsDTO(0, 0, 0));
        }

        final Pageable pageable = PageRequest.of(query.page(), query.size(), MOST_RECENT_ACTIVITY);
        final Page<CourseEnrollment> result = findPage(student, query.status(), pageable);
        final List<CourseEnrollmentDTO> items = result.getContent().stream().map(CourseEnrollment::toDTO).toList();
        final CourseEnrollmentCountsDTO counts = new CourseEnrollmentCountsDTO(
                repository.countByStudentAndArchivedFalseAndCompletionStatus(student, CompletionStatus.IN_PROGRESS),
                repository.countByStudentAndArchivedFalseAndCompletionStatus(student, CompletionStatus.COMPLETED),
                repository.countByStudentAndArchivedTrue(student));

        return new CourseEnrollmentPageDTO(items, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), counts);
    }

    private Page<CourseEnrollment> findPage(Student student, EnrollmentStatusFilter status, Pageable pageable) {
        if (status == null) {
            return repository.findByStudent(student, pageable);
        }

        return switch (status) {
            case IN_PROGRESS -> repository.findByStudentAndArchivedFalseAndCompletionStatus(student, CompletionStatus.IN_PROGRESS, pageable);
            case COMPLETED -> repository.findByStudentAndArchivedFalseAndCompletionStatus(student, CompletionStatus.COMPLETED, pageable);
            case ARCHIVED -> repository.findByStudentAndArchivedTrue(student, pageable);
        };
    }
}
