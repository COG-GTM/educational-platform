package com.educational.platform.courses.course.approve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.retry.IntegrationEventRetryPolicy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringJUnitConfig(CourseApprovedByAdminRetryableInvokerTest.TestConfig.class)
class CourseApprovedByAdminRetryableInvokerTest {

    @MockitoBean
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @Autowired
    private CourseApprovedByAdminRetryableInvoker sut;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(CourseApprovedByAdminRetryableInvoker.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void invoke_success_commandBuiltFromEventCourseIdAndSingleAttempt() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(uuid);

        sut.invoke(event);

        final ArgumentCaptor<ApproveCourseCommand> argument = ArgumentCaptor.forClass(ApproveCourseCommand.class);
        verify(approveCourseCommandHandler, times(1)).handle(argument.capture());
        assertThat(argument.getValue()).hasFieldOrPropertyWithValue("uuid", uuid);
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void invoke_transientFailureThenSuccess_retriesAndReturns() {
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final AtomicInteger attempts = new AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            if (attempts.getAndIncrement() < IntegrationEventRetryPolicy.MAX_ATTEMPTS - 1) {
                throw new RuntimeException("transient");
            }
            return null;
        }).when(approveCourseCommandHandler).handle(any());

        assertThatCode(() -> sut.invoke(event)).doesNotThrowAnyException();
        verify(approveCourseCommandHandler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle(any());
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void invoke_exhaustion_logsErrorAndReturns() {
        final CourseApprovedByAdminIntegrationEvent event = new CourseApprovedByAdminIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(approveCourseCommandHandler).handle(any());

        assertThatCode(() -> sut.invoke(event)).doesNotThrowAnyException();
        verify(approveCourseCommandHandler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle(any());

        assertThat(listAppender.list).hasSize(1);
        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage()).contains("Integration event 'CourseApprovedByAdminIntegrationEvent' exhausted retries");
        assertThat(loggingEvent.getFormattedMessage()).contains(event.toString());
        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Configuration
    @EnableRetry
    @Import(CourseApprovedByAdminRetryableInvoker.class)
    static class TestConfig {
    }
}
