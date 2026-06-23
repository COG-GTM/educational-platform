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

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that all handlers correctly persist Unicode exception messages in dead-letter
 * records. Database error messages may contain non-ASCII characters in internationalized
 * environments (e.g., PostgreSQL with locale-specific messages, Oracle with NLS settings).
 * The {@code exception_message} column (VARCHAR 2000) must handle these correctly.
 */
class IntegrationEventHandlerRecoverUnicodeTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveHandler_recover_cjkExceptionMessage_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        String cjkMessage = "数据库连接失败：连接超时 (タイムアウト) 데이터베이스";
        var exception = new DataAccessResourceFailureException(cjkMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, cjkMessage);
    }

    @Test
    void courseApprovedHandler_recover_cyrillicExceptionMessage_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseApprovedByAdminIntegrationEventHandler(
                mock(ApproveCourseCommandHandler.class), repo);
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        String cyrillicMessage = "Ошибка базы данных: соединение отклонено сервером";
        var exception = new DataAccessResourceFailureException(cyrillicMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, cyrillicMessage);
    }

    @Test
    void studentEnrolledHandler_recover_mixedScriptExceptionMessage_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new StudentEnrolledToCourseIntegrationEventHandler(
                mock(IncreaseNumberOfStudentsCommandHandler.class), repo);
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        String mixedMessage = "Error 数据库 at host=db.example.com: Ошибка подключения (code: 50003)";
        var exception = new DataAccessResourceFailureException(mixedMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, mixedMessage);
    }

    @Test
    void courseRatingHandler_recover_arabicExceptionMessage_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new CourseRatingRecalculatedIntegrationEventHandler(
                mock(UpdateCourseRatingCommandHandler.class), repo);
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        String arabicMessage = "خطأ في قاعدة البيانات: تم رفض الاتصال";
        var exception = new DataAccessResourceFailureException(arabicMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, arabicMessage);
    }

    @Test
    void userCreatedHandler_recover_emojiExceptionMessage_preserved() throws Exception {
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new UserCreatedIntegrationEventHandler(
                mock(CreateTeacherCommandHandler.class), repo);
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        String emojiMessage = "Connection failed \uD83D\uDEA8 timeout after 30s \u26A0\uFE0F retry exhausted";
        var exception = new DataAccessResourceFailureException(emojiMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, emojiMessage);
    }

    @Test
    void allHandlers_recover_supplementaryUnicodeCharacters_preserved() throws Exception {
        // Supplementary characters (outside BMP) — important for VARCHAR column handling
        var repo = mock(FailedIntegrationEventRepository.class);
        var handler = new SendCourseToApproveIntegrationEventHandler(
                mock(CreateCourseProposalCommandHandler.class), repo);
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        String supplementaryMessage = "Error: \uD835\uDC00\uD835\uDC01\uD835\uDC02 (mathematical bold)";
        var exception = new DataAccessResourceFailureException(supplementaryMessage);

        handler.recover(exception, event);

        assertPersistedMessage(repo, supplementaryMessage);
    }

    // --- Helpers ---

    private void assertPersistedMessage(FailedIntegrationEventRepository repo,
                                         String expectedMessage) throws Exception {
        var captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(repo).save(captor.capture());
        Field field = FailedIntegrationEventRecord.class.getDeclaredField("exceptionMessage");
        field.setAccessible(true);
        assertThat(field.get(captor.getValue())).isEqualTo(expectedMessage);
    }
}
