package com.educational.platform.config.handler;

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
 * Verifies that all integration event records expose their data correctly via
 * record accessor methods, and that the data round-trips through construction
 * without loss. This guards against accidental field reordering in record
 * definitions (which would silently swap parameters).
 * <p>
 * Also verifies immutability: records cannot be modified after construction.
 */
class IntegrationEventRecordAccessorConsistencyTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void sendCourseToApproveEvent_courseIdAccessor_returnsConstructorValue() {
        var event = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        assertThat(event.courseId()).isEqualTo(COURSE_ID);
    }

    @Test
    void courseApprovedByAdminEvent_courseIdAccessor_returnsConstructorValue() {
        var event = new CourseApprovedByAdminIntegrationEvent(COURSE_ID);
        assertThat(event.courseId()).isEqualTo(COURSE_ID);
    }

    @Test
    void studentEnrolledToCourseEvent_accessors_returnConstructorValues() {
        var event = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        assertThat(event.courseId()).isEqualTo(COURSE_ID);
        assertThat(event.username()).isEqualTo("student1");
    }

    @Test
    void courseRatingRecalculatedEvent_accessors_returnConstructorValues() {
        var event = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        assertThat(event.courseId()).isEqualTo(COURSE_ID);
        assertThat(event.rating()).isEqualTo(4.5);
    }

    @Test
    void userCreatedEvent_accessors_returnConstructorValues() {
        var event = new UserCreatedIntegrationEvent("teacher1", "teacher1@test.com");
        assertThat(event.username()).isEqualTo("teacher1");
        assertThat(event.email()).isEqualTo("teacher1@test.com");
    }

    @Test
    void sendCourseToApproveEvent_withNullCourseId_accessorReturnsNull() {
        var event = new SendCourseToApproveIntegrationEvent(null);
        assertThat(event.courseId()).isNull();
    }

    @Test
    void courseApprovedByAdminEvent_withNullCourseId_accessorReturnsNull() {
        var event = new CourseApprovedByAdminIntegrationEvent(null);
        assertThat(event.courseId()).isNull();
    }

    @Test
    void studentEnrolledToCourseEvent_withNullValues_accessorsReturnNull() {
        var event = new StudentEnrolledToCourseIntegrationEvent(null, null);
        assertThat(event.courseId()).isNull();
        assertThat(event.username()).isNull();
    }

    @Test
    void userCreatedEvent_withNullValues_accessorsReturnNull() {
        var event = new UserCreatedIntegrationEvent(null, null);
        assertThat(event.username()).isNull();
        assertThat(event.email()).isNull();
    }

    @Test
    void courseRatingRecalculatedEvent_withSpecialDoubleValues() {
        // NaN
        var nanEvent = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.NaN);
        assertThat(nanEvent.rating()).isNaN();

        // Positive infinity
        var infEvent = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, Double.POSITIVE_INFINITY);
        assertThat(infEvent.rating()).isEqualTo(Double.POSITIVE_INFINITY);

        // Negative value
        var negEvent = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, -1.0);
        assertThat(negEvent.rating()).isEqualTo(-1.0);

        // Zero
        var zeroEvent = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 0.0);
        assertThat(zeroEvent.rating()).isZero();
    }

    @Test
    void allCourseIdEvents_sameUUID_producesEqualEvents() {
        var event1 = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var event2 = new SendCourseToApproveIntegrationEvent(COURSE_ID);

        // Records override equals based on component values
        assertThat(event1).isEqualTo(event2);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    void allCourseIdEvents_differentUUID_producesUnequalEvents() {
        UUID otherId = UUID.fromString("987fcdeb-51a2-43e7-b890-123456789abc");
        var event1 = new SendCourseToApproveIntegrationEvent(COURSE_ID);
        var event2 = new SendCourseToApproveIntegrationEvent(otherId);

        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    void userCreatedEvent_equalityBasedOnBothFields() {
        var event1 = new UserCreatedIntegrationEvent("user1", "user1@test.com");
        var event2 = new UserCreatedIntegrationEvent("user1", "user1@test.com");
        var event3 = new UserCreatedIntegrationEvent("user1", "different@test.com");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    @Test
    void studentEnrolledEvent_equalityBasedOnBothFields() {
        var event1 = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var event2 = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student1");
        var event3 = new StudentEnrolledToCourseIntegrationEvent(COURSE_ID, "student2");

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    @Test
    void courseRatingEvent_equalityBasedOnBothFields() {
        var event1 = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var event2 = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 4.5);
        var event3 = new CourseRatingRecalculatedIntegrationEvent(COURSE_ID, 3.0);

        assertThat(event1).isEqualTo(event2);
        assertThat(event1).isNotEqualTo(event3);
    }

    @ParameterizedTest
    @MethodSource("allEventTypes")
    void allEvents_areJavaRecords(Class<?> eventType) {
        assertThat(eventType.isRecord())
                .as("%s should be a Java record", eventType.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("allEventTypes")
    void allEvents_classNameEndsWithIntegrationEvent(Class<?> eventType) {
        assertThat(eventType.getSimpleName())
                .endsWith("IntegrationEvent");
    }

    @ParameterizedTest
    @MethodSource("allEventTypes")
    void allEvents_haveAtLeastOneRecordComponent(Class<?> eventType) {
        assertThat(eventType.getRecordComponents())
                .as("%s should have at least one record component", eventType.getSimpleName())
                .isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("allEventTypes")
    void allEvents_areFinal(Class<?> eventType) {
        // Java records are implicitly final
        assertThat(java.lang.reflect.Modifier.isFinal(eventType.getModifiers()))
                .as("%s should be final (records are implicitly final)", eventType.getSimpleName())
                .isTrue();
    }

    static Stream<Arguments> allEventTypes() {
        return Stream.of(
                Arguments.of(SendCourseToApproveIntegrationEvent.class),
                Arguments.of(CourseApprovedByAdminIntegrationEvent.class),
                Arguments.of(StudentEnrolledToCourseIntegrationEvent.class),
                Arguments.of(CourseRatingRecalculatedIntegrationEvent.class),
                Arguments.of(UserCreatedIntegrationEvent.class)
        );
    }
}
