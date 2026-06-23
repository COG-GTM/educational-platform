package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.create.CreateTeacherCommandHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

/**
 * Verifies that the event and exception class names persisted in dead-letter records
 * are resolvable via {@link Class#forName(String)}. This is critical for automated
 * retry tooling that reads dead-letter records and needs to reconstruct event types
 * or categorize exception types programmatically.
 */
class IntegrationEventHandlerDeadLetterResolvabilityTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApprove_recover_eventClassNameResolvableViaReflection() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException("DB error"),
                new SendCourseToApproveIntegrationEvent(COURSE_ID));

        var record = captureRecord(repo);
        assertClassNameResolvable((String) getField(record, "eventClassName"));
    }

    @Test
    void courseApprovedByAdmin_recover_eventClassNameResolvableViaReflection() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException("DB error"),
                new CourseApprovedByAdminIntegrationEvent(COURSE_ID));

        var record = captureRecord(repo);
        assertClassNameResolvable((String) getField(record, "eventClassName"));
    }

    @Test
    void studentEnrolledToCourse_recover_eventClassNameResolvableViaReflection() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException("DB error"),
                new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1"));

        var record = captureRecord(repo);
        assertClassNameResolvable((String) getField(record, "eventClassName"));
    }

    @Test
    void courseRatingRecalculated_recover_eventClassNameResolvableViaReflection() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException("DB error"),
                new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5));

        var record = captureRecord(repo);
        assertClassNameResolvable((String) getField(record, "eventClassName"));
    }

    @Test
    void userCreated_recover_eventClassNameResolvableViaReflection() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        handler.recover(new DataAccessResourceFailureException("DB error"),
                new UserCreatedIntegrationEvent("teacher1", "teacher1@edu.com"));

        var record = captureRecord(repo);
        assertClassNameResolvable((String) getField(record, "eventClassName"));
    }

    @Test
    void recover_dataAccessResourceFailure_exceptionClassNameResolvable() throws Exception {
        var record = recoverSendCourseToApprove(new DataAccessResourceFailureException("error"));
        assertClassNameResolvable((String) getField(record, "exceptionClassName"));
    }

    @Test
    void recover_dataIntegrityViolation_exceptionClassNameResolvable() throws Exception {
        var record = recoverSendCourseToApprove(new DataIntegrityViolationException("constraint"));
        assertClassNameResolvable((String) getField(record, "exceptionClassName"));
    }

    @Test
    void recover_optimisticLockingFailure_exceptionClassNameResolvable() throws Exception {
        var record = recoverSendCourseToApprove(
                new ObjectOptimisticLockingFailureException("lock", new RuntimeException()));
        assertClassNameResolvable((String) getField(record, "exceptionClassName"));
    }

    @Test
    void recover_resolvedEventClassMatchesOriginalEventType() throws Exception {
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var record = recoverSendCourseToApprove(new DataAccessResourceFailureException("error"));

        String eventClassName = (String) getField(record, "eventClassName");
        Class<?> resolvedClass = Class.forName(eventClassName);
        assertThat(resolvedClass).isEqualTo(event.getClass());
    }

    @Test
    void recover_resolvedExceptionClassIsAssignableFromDataAccessException() throws Exception {
        var record = recoverSendCourseToApprove(new DataAccessResourceFailureException("error"));

        String exceptionClassName = (String) getField(record, "exceptionClassName");
        Class<?> resolvedClass = Class.forName(exceptionClassName);
        assertThat(org.springframework.dao.DataAccessException.class.isAssignableFrom(resolvedClass))
                .as("Resolved exception class should be a DataAccessException subclass")
                .isTrue();
    }

    private FailedIntegrationEventRecord recoverSendCourseToApprove(
            org.springframework.dao.DataAccessException exception) throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        handler.recover(exception, new SendCourseToApproveIntegrationEvent(COURSE_ID));
        return captureRecord(repo);
    }

    private FailedIntegrationEventRecord captureRecord(FailedIntegrationEventRepository repo) {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        return captor.getValue();
    }

    private void assertClassNameResolvable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("Class name '%s' should be resolvable via Class.forName()", className)
                .doesNotThrowAnyException();
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
