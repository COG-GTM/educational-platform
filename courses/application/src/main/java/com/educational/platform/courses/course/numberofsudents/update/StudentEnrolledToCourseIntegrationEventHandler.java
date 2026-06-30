package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
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

    private static final Logger logger = LoggerFactory.getLogger(StudentEnrolledToCourseIntegrationEventHandler.class);

    private final IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public StudentEnrolledToCourseIntegrationEventHandler(IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler,
                                                          FailedIntegrationEventRepository failedEventRepository) {
        this.increaseNumberOfStudentsCommandHandler = increaseNumberOfStudentsCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @EventListener
    @Retryable(retryFor = { DataAccessException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        logger.info("Received StudentEnrolledToCourseIntegrationEvent for course {}", event.courseId());
        try {
            increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(event.courseId()));
        } catch (Exception ex) {
            logger.error("Failed to handle StudentEnrolledToCourseIntegrationEvent {}", event, ex);
            throw ex;
        }
    }

    @Recover
    public void recover(Throwable ex, StudentEnrolledToCourseIntegrationEvent event) {
        logger.error("Exhausted retries for StudentEnrolledToCourseIntegrationEvent {}", event, ex);
        failedEventRepository.save(FailedIntegrationEvent.of(
                event.getClass().getName(),
                String.valueOf(event),
                ex.getMessage(),
                IntegrationEventRetryHandler.MAX_ATTEMPTS));
    }

}
