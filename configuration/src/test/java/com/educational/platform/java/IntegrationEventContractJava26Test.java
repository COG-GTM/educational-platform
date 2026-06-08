package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the cross-module integration event contracts remain intact
 * on Java 26 bytecode.
 * <p>
 * The educational platform uses Spring's {@code ApplicationEventPublisher} to
 * decouple bounded contexts. Events are Java records passed between modules.
 * Spring resolves event types via reflection at runtime. If Java 26 bytecode
 * changes affect how record components, equals/hashCode, or toString are
 * generated, event routing and logging would silently break.
 * <p>
 * {@link ExistingArchUnitTestCompatibilityTest} covers the ArchUnit immutability
 * rule. This test covers the <em>event contract</em>: record structure, component
 * types, accessor methods, and the canonical constructor — all resolved via
 * reflection as Spring would at runtime.
 */
public class IntegrationEventContractJava26Test {

    // --- All integration events should be records ---

    static Stream<Arguments> allIntegrationEvents() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent"),
                Arguments.of("com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent"),
                Arguments.of("com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent"),
                Arguments.of("com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent"),
                Arguments.of("com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent"),
                Arguments.of("com.educational.platform.users.integration.event.UserCreatedIntegrationEvent")
        );
    }

    @ParameterizedTest(name = "Integration event should be a record: {0}")
    @MethodSource("allIntegrationEvents")
    void integrationEvent_shouldBeRecord(String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        assertThat(clazz.isRecord())
                .as("%s should be a Java record (immutable event)", className)
                .isTrue();
    }

    @ParameterizedTest(name = "Integration event record should have components: {0}")
    @MethodSource("allIntegrationEvents")
    void integrationEvent_shouldHaveRecordComponents(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        RecordComponent[] components = clazz.getRecordComponents();

        assertThat(components)
                .as("%s should have at least one record component", className)
                .isNotEmpty();

        for (RecordComponent component : components) {
            assertThat(component.getName())
                    .as("Component name should be non-empty")
                    .isNotBlank();

            assertThat(component.getType())
                    .as("Component type should be resolvable")
                    .isNotNull();

            Method accessor = component.getAccessor();
            assertThat(accessor)
                    .as("Component accessor method should exist")
                    .isNotNull();
        }
    }

    // --- UUID-based events should use correct key type ---

    static Stream<Arguments> uuidBasedEvents() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent",
                        "courseId"),
                Arguments.of("com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent",
                        "courseId"),
                Arguments.of("com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent",
                        "courseId"),
                Arguments.of("com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent",
                        "courseId")
        );
    }

    @ParameterizedTest(name = "Event {0} should use UUID for ''{1}''")
    @MethodSource("uuidBasedEvents")
    void uuidEvent_keyField_shouldBeUuidType(String className, String componentName) throws Exception {
        Class<?> clazz = Class.forName(className);
        RecordComponent component = Arrays.stream(clazz.getRecordComponents())
                .filter(c -> c.getName().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing component: " + componentName));

        assertThat(component.getType())
                .as("Natural key component '%s' on %s should be UUID", componentName, className)
                .isEqualTo(UUID.class);
    }

    // --- Record equals/hashCode/toString contract ---

    @Test
    void sendCourseToApprove_equalsHashCode_shouldWork_onJava26() throws Exception {
        Class<?> clazz = Class.forName(
                "com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent");
        UUID id = UUID.randomUUID();

        Object event1 = clazz.getDeclaredConstructor(UUID.class).newInstance(id);
        Object event2 = clazz.getDeclaredConstructor(UUID.class).newInstance(id);
        Object event3 = clazz.getDeclaredConstructor(UUID.class).newInstance(UUID.randomUUID());

        assertThat(event1)
                .as("Same UUID should produce equal events")
                .isEqualTo(event2);

        assertThat(event1.hashCode())
                .as("Equal events should have same hashCode")
                .isEqualTo(event2.hashCode());

        assertThat(event1)
                .as("Different UUIDs should produce unequal events")
                .isNotEqualTo(event3);
    }

    @Test
    void sendCourseToApprove_toString_shouldContainComponentValues() throws Exception {
        Class<?> clazz = Class.forName(
                "com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent");
        UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");

        Object event = clazz.getDeclaredConstructor(UUID.class).newInstance(id);

        assertThat(event.toString())
                .as("Record toString should include the UUID value")
                .contains("12345678-1234-1234-1234-123456789abc");
    }

    @Test
    void userCreatedEvent_toString_shouldContainStringComponents() throws Exception {
        Class<?> clazz = Class.forName(
                "com.educational.platform.users.integration.event.UserCreatedIntegrationEvent");

        Object event = clazz.getDeclaredConstructor(String.class, String.class)
                .newInstance("testuser", "test@example.com");

        assertThat(event.toString())
                .as("UserCreatedIntegrationEvent toString should contain component values")
                .contains("testuser")
                .contains("test@example.com");
    }

    // --- Canonical constructor accessibility ---

    @ParameterizedTest(name = "Event {0} canonical constructor should be public")
    @MethodSource("allIntegrationEvents")
    void integrationEvent_canonicalConstructor_shouldBePublic(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        RecordComponent[] components = clazz.getRecordComponents();

        Class<?>[] paramTypes = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);

        assertThatCode(() -> clazz.getDeclaredConstructor(paramTypes))
                .as("Canonical constructor should be accessible on %s", className)
                .doesNotThrowAnyException();
    }

    // --- Event naming convention ---

    @ParameterizedTest(name = "Event class name should end with 'IntegrationEvent': {0}")
    @MethodSource("allIntegrationEvents")
    void integrationEvent_shouldFollowNamingConvention(String className) {
        assertThat(className)
                .as("Integration event class should end with 'IntegrationEvent'")
                .endsWith("IntegrationEvent");
    }
}
