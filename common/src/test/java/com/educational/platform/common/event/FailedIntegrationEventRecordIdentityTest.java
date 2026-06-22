package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the identity and equality behavior of {@link FailedIntegrationEventRecord}.
 * <p>
 * JPA entities that do not override {@code equals()}/{@code hashCode()} use object
 * identity (reference equality). This is the intended design for this entity:
 * <ul>
 *   <li>Records are write-once (no updates after initial persistence)</li>
 *   <li>Records are never compared in application code (only queried by ID)</li>
 *   <li>No business key exists (the combination of fields is the "key" but isn't unique)</li>
 * </ul>
 * These tests document and guard this contract so that accidental equals/hashCode
 * additions don't break Set/Map behavior in JPA managed collections.
 */
class FailedIntegrationEventRecordIdentityTest {

    @Test
    void twoRecordsWithSameData_areNotEqual() {
        // given — identical constructor arguments
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        // then — reference equality (no equals override)
        assertThat(record1).isNotEqualTo(record2);
        assertThat(record1).isNotSameAs(record2);
    }

    @Test
    void sameRecord_isEqualToItself() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(record).isEqualTo(record);
        assertThat(record).isSameAs(record);
    }

    @Test
    void hashCode_usesObjectIdentity() {
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        // Different objects → likely different hashCodes (identity-based)
        // Note: technically could collide, but statistically won't
        assertThat(record1.hashCode()).isEqualTo(System.identityHashCode(record1));
        assertThat(record2.hashCode()).isEqualTo(System.identityHashCode(record2));
    }

    @Test
    void equals_notOverridden_usesObjectEquals() throws NoSuchMethodException {
        // Verify equals is inherited from Object (not overridden)
        assertThat(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .as("FailedIntegrationEventRecord should NOT override equals()")
                .noneMatch(m -> m.getName().equals("equals"));
    }

    @Test
    void hashCode_notOverridden_usesObjectHashCode() {
        // Verify hashCode is inherited from Object (not overridden)
        assertThat(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .as("FailedIntegrationEventRecord should NOT override hashCode()")
                .noneMatch(m -> m.getName().equals("hashCode"));
    }

    @Test
    void toString_notOverridden_usesObjectToString() {
        // Verify toString is not overridden (entity has private fields, no reason to expose)
        assertThat(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .as("FailedIntegrationEventRecord should NOT override toString()")
                .noneMatch(m -> m.getName().equals("toString"));
    }

    @Test
    void record_isNotEqualToNull() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(record).isNotEqualTo(null);
    }

    @Test
    void record_isNotEqualToDifferentType() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(record).isNotEqualTo("some string");
        assertThat(record).isNotEqualTo(42);
    }

    @Test
    void multipleInstances_inCollection_maintainIndependentIdentity() {
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        java.util.Set<FailedIntegrationEventRecord> set = new java.util.HashSet<>();
        set.add(record1);
        set.add(record2);

        // Both should be in the set (identity-based, not value-based)
        assertThat(set).hasSize(2).contains(record1, record2);
    }

    @Test
    void fieldsAreEncapsulated_noPublicAccessors() {
        // Ensure the record remains opaque — no way to extract data except reflection or JPA
        long publicMethodCount = java.util.Arrays.stream(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.getName().equals("equals") && !m.getName().equals("hashCode")
                        && !m.getName().equals("toString"))
                .count();
        assertThat(publicMethodCount)
                .as("No public methods beyond Object methods — entity is fully encapsulated")
                .isZero();
    }

    @Test
    void constructedRecord_fieldsCannotBeModifiedThroughReflection_without_setAccessible() {
        // All fields are private — normal access is blocked
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "java.lang.RuntimeException", 3);

        for (Field field : FailedIntegrationEventRecord.class.getDeclaredFields()) {
            assertThat(field.canAccess(record))
                    .as("Field '%s' should not be accessible without setAccessible", field.getName())
                    .isFalse();
        }
    }
}
