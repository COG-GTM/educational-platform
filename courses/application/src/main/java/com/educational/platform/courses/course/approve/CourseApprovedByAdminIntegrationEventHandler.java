package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link CourseApprovedByAdminIntegrationEvent}.
 */
@Component
public class CourseApprovedByAdminIntegrationEventHandler {

    private final CourseApprovedByAdminRetryableInvoker courseApprovedByAdminRetryableInvoker;

    public CourseApprovedByAdminIntegrationEventHandler(CourseApprovedByAdminRetryableInvoker courseApprovedByAdminRetryableInvoker) {
        this.courseApprovedByAdminRetryableInvoker = courseApprovedByAdminRetryableInvoker;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseApprovedByAdminEvent(CourseApprovedByAdminIntegrationEvent event) {
        courseApprovedByAdminRetryableInvoker.invoke(event);
    }

}
