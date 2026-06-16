package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Command handler for {@link UpdateCourseRatingCommand} updates a rating course.
 */
@Component
@Transactional
public class UpdateCourseRatingCommandHandler {

    private final CourseRepository repository;

    public UpdateCourseRatingCommandHandler(CourseRepository repository) {
        this.repository = repository;
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void handle(UpdateCourseRatingCommand command) {
        final Optional<Course> dbResult = repository.findByUuid(command.uuid());
        if (dbResult.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Course with uuid: %s not found", command.uuid()));
        }

        final Course course = dbResult.get();
        course.updateRating(command.rating());
        repository.save(course);
    }

}
