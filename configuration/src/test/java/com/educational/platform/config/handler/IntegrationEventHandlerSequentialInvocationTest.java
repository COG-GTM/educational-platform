package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.course.create.CreateCourseProposalCommandHandler;
import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.courses.course.approve.ApproveCourseCommand;
import com.educational.platform.courses.course.approve.ApproveCourseCommandHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;
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

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies that handler instances process multiple sequential events independently.
 * <p>
 * Handlers are singletons in the Spring context and receive events on the async thread
 * pool. This test simulates sequential invocations on the same instance to verify:
 * <ul>
 *   <li>No state leakage between invocations (each event is processed with its own data)</li>
 *   <li>The command handler receives the correct arguments for each invocation</li>
 *   <li>The recover path produces independent dead-letter records per invocation</li>
 * </ul>
 */
class IntegrationEventHandlerSequentialInvocationTest {

    private static final UUID COURSE_ID_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COURSE_ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COURSE_ID_3 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void sendCourseToApproveHandler_multipleEvents_eachProcessedIndependently() {
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);

        handler.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID_1));
        handler.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID_2));
        handler.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID_3));

        var captor = ArgumentCaptor.forClass(CreateCourseProposalCommand.class);
        verify(commandHandler, times(3)).handle(captor.capture());
        var commands = captor.getAllValues();
        assertThat(commands.get(0)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_1);
        assertThat(commands.get(1)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_2);
        assertThat(commands.get(2)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_3);
    }

    @Test
    void courseApprovedByAdminHandler_multipleEvents_eachProcessedIndependently() {
        var commandHandler = mock(ApproveCourseCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(commandHandler, repo);

        handler.handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(COURSE_ID_1));
        handler.handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(COURSE_ID_2));
        handler.handleCourseApprovedByAdminEvent(new CourseApprovedByAdminIntegrationEvent(COURSE_ID_3));

        var captor = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(commandHandler, times(3)).handle(captor.capture());
        var commands = captor.getAllValues();
        assertThat(commands.get(0)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_1);
        assertThat(commands.get(1)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_2);
        assertThat(commands.get(2)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_3);
    }

    @Test
    void studentEnrolledHandler_multipleEvents_eachProcessedIndependently() {
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repo);

        handler.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID_1, "student1"));
        handler.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID_2, "student2"));
        handler.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID_3, "student3"));

        var captor = ArgumentCaptor.forClass(IncreaseNumberOfStudentsCommand.class);
        verify(commandHandler, times(3)).handle(captor.capture());
        var commands = captor.getAllValues();
        assertThat(commands.get(0)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_1);
        assertThat(commands.get(1)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_2);
        assertThat(commands.get(2)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_3);
    }

    @Test
    void courseRatingHandler_multipleEvents_eachProcessedIndependently() {
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(commandHandler, repo);

        handler.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID_1, 3.5));
        handler.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID_2, 4.0));
        handler.handleCourseRatingRecalculatedEvent(new CourseRatingRecalculatedIntegrationEvent(COURSE_ID_3, 5.0));

        var captor = ArgumentCaptor.forClass(UpdateCourseRatingCommand.class);
        verify(commandHandler, times(3)).handle(captor.capture());
        var commands = captor.getAllValues();
        assertThat(commands.get(0)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_1).hasFieldOrPropertyWithValue("rating", 3.5);
        assertThat(commands.get(1)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_2).hasFieldOrPropertyWithValue("rating", 4.0);
        assertThat(commands.get(2)).hasFieldOrPropertyWithValue("uuid", COURSE_ID_3).hasFieldOrPropertyWithValue("rating", 5.0);
    }

    @Test
    void userCreatedHandler_multipleEvents_eachProcessedIndependently() {
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(commandHandler, repo);

        handler.handleUserCreatedEvent(new UserCreatedIntegrationEvent("user1", "user1@test.com"));
        handler.handleUserCreatedEvent(new UserCreatedIntegrationEvent("user2", "user2@test.com"));
        handler.handleUserCreatedEvent(new UserCreatedIntegrationEvent("user3", "user3@test.com"));

        var captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(commandHandler, times(3)).handle(captor.capture());
        var commands = captor.getAllValues();
        assertThat(commands.get(0)).hasFieldOrPropertyWithValue("username", "user1");
        assertThat(commands.get(1)).hasFieldOrPropertyWithValue("username", "user2");
        assertThat(commands.get(2)).hasFieldOrPropertyWithValue("username", "user3");
    }

    @Test
    void sendCourseToApproveHandler_multipleRecovers_produceIndependentRecords() throws Exception {
        var commandHandler = mock(CreateCourseProposalCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(commandHandler, repo);

        handler.recover(new DataAccessResourceFailureException("error-1"), new SendCourseToApproveIntegrationEvent(COURSE_ID_1));
        handler.recover(new DataAccessResourceFailureException("error-2"), new SendCourseToApproveIntegrationEvent(COURSE_ID_2));
        handler.recover(new DataAccessResourceFailureException("error-3"), new SendCourseToApproveIntegrationEvent(COURSE_ID_3));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(3)).save(captor.capture());
        var records = captor.getAllValues();

        assertThat(getField(records.get(0), "exceptionMessage")).isEqualTo("error-1");
        assertThat(getField(records.get(1), "exceptionMessage")).isEqualTo("error-2");
        assertThat(getField(records.get(2), "exceptionMessage")).isEqualTo("error-3");

        assertThat((String) getField(records.get(0), "eventPayload")).contains(COURSE_ID_1.toString());
        assertThat((String) getField(records.get(1), "eventPayload")).contains(COURSE_ID_2.toString());
        assertThat((String) getField(records.get(2), "eventPayload")).contains(COURSE_ID_3.toString());
    }

    @Test
    void courseRatingHandler_multipleRecovers_produceIndependentRecords() throws Exception {
        var commandHandler = mock(UpdateCourseRatingCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(commandHandler, repo);

        handler.recover(new DataAccessResourceFailureException("db-error-A"), new CourseRatingRecalculatedIntegrationEvent(COURSE_ID_1, 1.0));
        handler.recover(new DataAccessResourceFailureException("db-error-B"), new CourseRatingRecalculatedIntegrationEvent(COURSE_ID_2, 2.0));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo, times(2)).save(captor.capture());
        var records = captor.getAllValues();

        assertThat(getField(records.get(0), "exceptionMessage")).isEqualTo("db-error-A");
        assertThat(getField(records.get(1), "exceptionMessage")).isEqualTo("db-error-B");
        assertThat((String) getField(records.get(0), "eventPayload")).contains("1.0");
        assertThat((String) getField(records.get(1), "eventPayload")).contains("2.0");
    }

    @Test
    void userCreatedHandler_handleThenRecover_noStateLeakage() throws Exception {
        var commandHandler = mock(CreateTeacherCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(commandHandler, repo);

        // First: successful handle
        handler.handleUserCreatedEvent(new UserCreatedIntegrationEvent("success-user", "success@test.com"));
        verify(commandHandler, times(1)).handle(any());
        verifyNoInteractions(repo);

        // Then: recover (simulating retries exhausted on a different event)
        handler.recover(new DataAccessResourceFailureException("failed"), new UserCreatedIntegrationEvent("failed-user", "failed@test.com"));

        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        assertThat((String) getField(captor.getValue(), "eventPayload")).contains("failed-user");
        assertThat((String) getField(captor.getValue(), "eventPayload")).doesNotContain("success-user");
    }

    @Test
    void studentEnrolledHandler_handleThenRecover_commandHandlerNotInvokedDuringRecover() {
        var commandHandler = mock(IncreaseNumberOfStudentsCommandHandler.class);
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repo);

        handler.handleStudentEnrolledToCourseEvent(new StudentEnrolledToCourseIntegrationEvent(COURSE_ID_1, "student"));
        verify(commandHandler, times(1)).handle(any());

        reset(commandHandler);
        handler.recover(new DataAccessResourceFailureException("err"), new StudentEnrolledToCourseIntegrationEvent(COURSE_ID_2, "student2"));
        verifyNoInteractions(commandHandler);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
