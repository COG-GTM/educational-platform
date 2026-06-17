package com.educational.platform.courses.course.numberofsudents.update;

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
 * Command handler for {@link IncreaseNumberOfStudentsCommand} increases a number of students.
 */
@Component
@Transactional
public class IncreaseNumberOfStudentsCommandHandler {

    private final CourseRepository repository;

    public IncreaseNumberOfStudentsCommandHandler(CourseRepository repository) {
        this.repository = repository;
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void handle(IncreaseNumberOfStudentsCommand command) {
        final Optional<Course> dbResult = repository.findByUuid(command.uuid());
        if (dbResult.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Course with uuid: %s not found", command.uuid()));
        }

        final Course course = dbResult.get();
        course.increaseNumberOfStudents();
        repository.save(course);
    }

}
