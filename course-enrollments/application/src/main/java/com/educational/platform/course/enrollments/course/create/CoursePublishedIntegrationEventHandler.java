package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.courses.integration.event.CoursePublishedIntegrationEvent;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link CoursePublishedIntegrationEvent}, makes the published course available for enrollment.
 */
@Component
public class CoursePublishedIntegrationEventHandler {

    private final CreateEnrollmentCourseCommandHandler createEnrollmentCourseCommandHandler;

    public CoursePublishedIntegrationEventHandler(CreateEnrollmentCourseCommandHandler createEnrollmentCourseCommandHandler) {
        this.createEnrollmentCourseCommandHandler = createEnrollmentCourseCommandHandler;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleCoursePublishedEvent(CoursePublishedIntegrationEvent event) {
        final var lectures = event.lectures().stream()
                .map(lecture -> new CreateCourseCommand.CreateLectureCommand(lecture.uuid(), lecture.title(), lecture.serialNumber()))
                .toList();
        createEnrollmentCourseCommandHandler.handle(new CreateCourseCommand(event.courseId(), event.name(), lectures));
    }

}
