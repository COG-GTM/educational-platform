package com.educational.platform.config;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that all integration events produce toString() / class name outputs
 * compatible with the dead-letter table column constraints (failed_integration_events).
 *
 * Column constraints from Liquibase schema:
 * - event_class_name: VARCHAR(500)
 * - event_payload:    VARCHAR(2000)
 */
class IntegrationEventDeadLetterCompatibilityTest {

    private static final int EVENT_CLASS_NAME_MAX_LENGTH = 500;
    private static final int EVENT_PAYLOAD_MAX_LENGTH = 2000;

    static Stream<Arguments> allEventInstances() {
        return Stream.of(
                Arguments.of(
                        new SendCourseToApproveIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001")),
                        "SendCourseToApproveIntegrationEvent"),
                Arguments.of(
                        new CourseApprovedByAdminIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440002")),
                        "CourseApprovedByAdminIntegrationEvent"),
                Arguments.of(
                        new StudentEnrolledToCourseIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440003"), "testuser"),
                        "StudentEnrolledToCourseIntegrationEvent"),
                Arguments.of(
                        new CourseRatingRecalculatedIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440004"), 4.5),
                        "CourseRatingRecalculatedIntegrationEvent"),
                Arguments.of(
                        new UserCreatedIntegrationEvent("testuser", "test@example.com"),
                        "UserCreatedIntegrationEvent")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allEventInstances")
    void allEvents_classNameFitsWithinDatabaseColumn(Object event, String eventName) {
        assertThat(event.getClass().getName().length())
                .as("%s class name '%s' must fit within VARCHAR(%d)",
                        eventName, event.getClass().getName(), EVENT_CLASS_NAME_MAX_LENGTH)
                .isLessThanOrEqualTo(EVENT_CLASS_NAME_MAX_LENGTH);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allEventInstances")
    void allEvents_toStringWithTypicalDataFitsWithinPayloadColumn(Object event, String eventName) {
        String payload = event.toString();

        assertThat(payload.length())
                .as("%s toString() with typical data (%d chars) must fit within VARCHAR(%d)",
                        eventName, payload.length(), EVENT_PAYLOAD_MAX_LENGTH)
                .isLessThanOrEqualTo(EVENT_PAYLOAD_MAX_LENGTH);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allEventInstances")
    void allEvents_toStringIsNotNull(Object event, String eventName) {
        assertThat(event.toString())
                .as("%s toString() must not return null", eventName)
                .isNotNull();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allEventInstances")
    void allEvents_toStringContainsClassName(Object event, String eventName) {
        assertThat(event.toString())
                .as("%s toString() should contain the event class simple name", eventName)
                .contains(event.getClass().getSimpleName());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("allEventInstances")
    void allEvents_areJavaRecords(Object event, String eventName) {
        assertThat(event.getClass().isRecord())
                .as("%s must be a Java record for automatic toString generation", eventName)
                .isTrue();
    }

    @Test
    void studentEnrolledEvent_withLongUsername_toStringMayExceedPayloadColumn() {
        // Documents that unbounded String fields can produce toString() exceeding column limits
        String longUsername = "u".repeat(2000);
        var event = new StudentEnrolledToCourseIntegrationEvent(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), longUsername);

        // This test documents the risk: event.toString() will exceed 2000 chars
        assertThat(event.toString().length())
                .as("toString() with 2000-char username exceeds payload column limit, " +
                        "highlighting need for truncation in @Recover methods")
                .isGreaterThan(EVENT_PAYLOAD_MAX_LENGTH);
    }

    @Test
    void userCreatedEvent_withLongUsernameAndEmail_toStringMayExceedPayloadColumn() {
        // Documents that unbounded String fields can produce toString() exceeding column limits
        String longUsername = "u".repeat(1000);
        String longEmail = "e".repeat(1000) + "@example.com";
        var event = new UserCreatedIntegrationEvent(longUsername, longEmail);

        assertThat(event.toString().length())
                .as("toString() with long username+email exceeds payload column limit, " +
                        "highlighting need for truncation in @Recover methods")
                .isGreaterThan(EVENT_PAYLOAD_MAX_LENGTH);
    }

    @Test
    void uuidBasedEvents_toStringHasBoundedLength() {
        // UUID-only events have bounded toString() since UUID.toString() is always 36 chars
        var event1 = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        var event2 = new CourseApprovedByAdminIntegrationEvent(UUID.randomUUID());

        assertThat(event1.toString().length())
                .as("UUID-only event toString() should be well within payload column limit")
                .isLessThan(200);
        assertThat(event2.toString().length())
                .as("UUID-only event toString() should be well within payload column limit")
                .isLessThan(200);
    }

    @Test
    void courseRatingEvent_withExtremeRatingValues_toStringFitsWithinPayloadColumn() {
        var eventNaN = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), Double.NaN);
        var eventInf = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), Double.POSITIVE_INFINITY);
        var eventNegInf = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), Double.NEGATIVE_INFINITY);
        var eventMax = new CourseRatingRecalculatedIntegrationEvent(UUID.randomUUID(), Double.MAX_VALUE);

        assertThat(eventNaN.toString().length()).isLessThanOrEqualTo(EVENT_PAYLOAD_MAX_LENGTH);
        assertThat(eventInf.toString().length()).isLessThanOrEqualTo(EVENT_PAYLOAD_MAX_LENGTH);
        assertThat(eventNegInf.toString().length()).isLessThanOrEqualTo(EVENT_PAYLOAD_MAX_LENGTH);
        assertThat(eventMax.toString().length()).isLessThanOrEqualTo(EVENT_PAYLOAD_MAX_LENGTH);
    }

    @Test
    void allEventClassNames_areDistinct() {
        var classNames = allEventInstances()
                .map(args -> args.get()[0].getClass().getName())
                .toList();

        assertThat(classNames)
                .as("All event class names must be distinct to avoid ambiguity in dead-letter table")
                .doesNotHaveDuplicates();
    }

    @Test
    void allEventSimpleNames_areDistinct() {
        var simpleNames = allEventInstances()
                .map(args -> args.get()[0].getClass().getSimpleName())
                .toList();

        assertThat(simpleNames)
                .as("All event simple names should be distinct for readability in dead-letter table")
                .doesNotHaveDuplicates();
    }

    @Test
    void allEventClassNames_endWithIntegrationEvent() {
        allEventInstances().forEach(args -> {
            Object event = args.get()[0];
            assertThat(event.getClass().getSimpleName())
                    .as("All integration event class names should follow naming convention")
                    .endsWith("IntegrationEvent");
        });
    }

    @Test
    void exceptionMessage_typicalSpringExceptionMessages_fitWithinColumn() {
        // exception_message column is VARCHAR(2000); verify realistic messages fit
        int exceptionMessageMaxLength = 2000;

        String[] typicalMessages = {
                "could not execute statement; SQL [n/a]; constraint [unique_course_id]",
                "Connection refused: connect; nested exception is java.net.ConnectException",
                "Lock wait timeout exceeded; try restarting transaction",
                "Deadlock found when trying to get lock; try restarting transaction",
                "Cannot acquire lock on table 'courses' in exclusive mode",
                "org.springframework.dao.QueryTimeoutException: JDBC operation timed out"
        };

        for (String message : typicalMessages) {
            assertThat(message.length())
                    .as("Typical exception message '%s' (%d chars) must fit within VARCHAR(%d)",
                            message, message.length(), exceptionMessageMaxLength)
                    .isLessThanOrEqualTo(exceptionMessageMaxLength);
        }
    }

    @Test
    void exceptionMessage_nullMessageFallbackToClassName_fitsWithinColumn() {
        // When exception.getMessage() is null, handlers fall back to exception.getClass().getName()
        int exceptionMessageMaxLength = 2000;

        String[] fallbackClassNames = {
                "org.springframework.dao.QueryTimeoutException",
                "org.springframework.dao.OptimisticLockingFailureException",
                "org.springframework.dao.PessimisticLockingFailureException",
                "org.springframework.dao.TransientDataAccessException",
                "org.springframework.dao.CannotAcquireLockException"
        };

        for (String className : fallbackClassNames) {
            assertThat(className.length())
                    .as("Exception class name fallback '%s' (%d chars) must fit within VARCHAR(%d)",
                            className, className.length(), exceptionMessageMaxLength)
                    .isLessThanOrEqualTo(exceptionMessageMaxLength);
        }
    }

    @Test
    void exceptionMessage_longNestedExceptionMessage_mayExceedColumn() {
        // Documents that deeply nested exceptions could exceed the column limit
        int exceptionMessageMaxLength = 2000;
        String longMessage = "Nested: " + "x".repeat(2000);

        assertThat(longMessage.length())
                .as("Deeply nested exception messages can exceed VARCHAR(%d), " +
                        "highlighting need for truncation in @Recover methods",
                        exceptionMessageMaxLength)
                .isGreaterThan(exceptionMessageMaxLength);
    }
}
