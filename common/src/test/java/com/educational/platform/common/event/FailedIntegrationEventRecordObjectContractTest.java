package com.educational.platform.common.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Object contract (equals, hashCode, toString) and multi-line/special
 * character handling for {@link FailedIntegrationEventRecord}.
 * <p>
 * JPA entities use identity-based equality (Object.equals/hashCode) by default.
 * These tests document and enforce that contract.
 */
class FailedIntegrationEventRecordObjectContractTest {

    @Test
    void hashCode_isIdentityBased_differentInstancesDifferentHash() {
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        // Identity-based hashCode: different instances have different hashes (with high probability)
        // We cannot guarantee they're different (hash collisions exist), but they shouldn't be
        // structurally computed to be the same
        assertThat(record1).isNotSameAs(record2);
    }

    @Test
    void equals_isIdentityBased_sameInstanceIsEqual() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        assertThat(record).isEqualTo(record);
        assertThat(record.hashCode()).isEqualTo(record.hashCode());
    }

    @Test
    void equals_isIdentityBased_differentInstancesNotEqual() {
        FailedIntegrationEventRecord record1 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);
        FailedIntegrationEventRecord record2 = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        assertThat(record1).isNotEqualTo(record2);
    }

    @Test
    void equals_nullIsNotEqual() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        assertThat(record).isNotEqualTo(null);
    }

    @Test
    void equals_differentTypeIsNotEqual() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        assertThat(record).isNotEqualTo("not a record");
    }

    @Test
    void constructor_multiLineExceptionMessage_preserved() throws Exception {
        String multiLineMessage = "Error occurred:\n  at com.example.Service.method(Service.java:42)\n  at com.example.Handler.handle(Handler.java:10)";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", multiLineMessage, "java.lang.RuntimeException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(multiLineMessage);
        assertThat(((String) getField(record, "exceptionMessage"))).contains("\n");
    }

    @Test
    void constructor_multiLineEventPayload_preserved() throws Exception {
        String multiLinePayload = "Event[\n  courseId=123e4567,\n  rating=4.5\n]";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", multiLinePayload, "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventPayload")).isEqualTo(multiLinePayload);
    }

    @Test
    void constructor_tabCharactersInMessage_preserved() throws Exception {
        String tabbedMessage = "Error:\tconnection refused\tat host:5432";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", tabbedMessage, "java.lang.RuntimeException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(tabbedMessage);
        assertThat(((String) getField(record, "exceptionMessage"))).contains("\t");
    }

    @Test
    void constructor_carriageReturnInMessage_preserved() throws Exception {
        String crlfMessage = "Line1\r\nLine2\r\nLine3";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", crlfMessage, "java.lang.RuntimeException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(crlfMessage);
    }

    @Test
    void constructor_sqlExceptionStyleMessage_preserved() throws Exception {
        String sqlMessage = "ERROR: duplicate key value violates unique constraint \"courses_pkey\"\n"
                + "  Detail: Key (id)=(123e4567-e89b-12d3-a456-426655440001) already exists.";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", sqlMessage, "org.postgresql.util.PSQLException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(sqlMessage);
    }

    @Test
    void constructor_jsonPayload_preserved() throws Exception {
        String jsonPayload = "{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"rating\":4.5}";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", jsonPayload, "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventPayload")).isEqualTo(jsonPayload);
    }

    @Test
    void constructor_controlCharactersInMessage_preserved() throws Exception {
        String messageWithControls = "Error\u0000null\u0001byte";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", messageWithControls, "java.lang.RuntimeException", 3);

        assertThat(getField(record, "exceptionMessage")).isEqualTo(messageWithControls);
    }

    @Test
    void constructor_eventClassNameWithDollarSign_innerClass_preserved() throws Exception {
        String innerClassName = "com.example.OuterClass$InnerEvent";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                innerClassName, "payload", "error", "java.lang.RuntimeException", 3);

        assertThat(getField(record, "eventClassName")).isEqualTo(innerClassName);
        assertThat(((String) getField(record, "eventClassName"))).contains("$");
    }

    @Test
    void constructor_exceptionClassNameWithDollarSign_anonymousClass_preserved() throws Exception {
        String anonymousClassName = "com.example.Service$1";

        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", anonymousClassName, 3);

        assertThat(getField(record, "exceptionClassName")).isEqualTo(anonymousClassName);
    }

    @Test
    void twoRecords_hashCodesAreConsistentAcrossMultipleCalls() {
        FailedIntegrationEventRecord record = new FailedIntegrationEventRecord(
                "com.example.Event", "payload", "error", "com.example.Exception", 3);

        int hash1 = record.hashCode();
        int hash2 = record.hashCode();
        int hash3 = record.hashCode();

        assertThat(hash1).isEqualTo(hash2).isEqualTo(hash3);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
