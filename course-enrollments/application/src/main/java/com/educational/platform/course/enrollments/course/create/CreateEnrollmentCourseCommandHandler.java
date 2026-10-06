package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.course.enrollments.course.EnrollCourse;
import com.educational.platform.course.enrollments.course.EnrollCourseRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Command handler for {@link CreateCourseCommand} creates a course or refreshes its snapshot when the course is published again.
 */
@Component
@Transactional
public class CreateEnrollmentCourseCommandHandler {

    private final EnrollCourseRepository courseRepository;

    public CreateEnrollmentCourseCommandHandler(EnrollCourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * Creates course from command or updates already known course.
     *
     * @param command command
     */
    public void handle(CreateCourseCommand command) {
        final EnrollCourse course = courseRepository.findByUuid(command.uuid())
                .map(existing -> existing.refresh(command))
                .orElseGet(() -> new EnrollCourse(command));
        courseRepository.save(course);
    }
}
