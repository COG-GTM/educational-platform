package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link StudentEnrolledToCourseIntegrationEvent}.
 */
@Component
public class StudentEnrolledToCourseIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(StudentEnrolledToCourseIntegrationEventHandler.class);

    private final IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public StudentEnrolledToCourseIntegrationEventHandler(IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler,
                                                           FailedIntegrationEventRepository failedEventRepository) {
        this.increaseNumberOfStudentsCommandHandler = increaseNumberOfStudentsCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @Retryable(retryFor = {TransientDataAccessException.class, OptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @EventListener
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        log.info("Received integration event: {}", event);
        try {
            increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(event.courseId()));
        } catch (Exception e) {
            log.error("Failed to handle integration event: {}", event, e);
            throw e;
        }
    }

    @Recover
    public void recover(TransientDataAccessException e, StudentEnrolledToCourseIntegrationEvent event) {
        log.error("All retries exhausted for event: {}. Error: {}", event, e.getMessage(), e);
        failedEventRepository.save(new FailedIntegrationEventRecord(
                event.getClass().getName(),
                event.toString(),
                e.getMessage() != null ? e.getMessage() : e.getClass().getName(),
                3
        ));
    }

}
