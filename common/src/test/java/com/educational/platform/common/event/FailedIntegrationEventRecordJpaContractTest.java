package com.educational.platform.common.event;

import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the JPA contract of {@link FailedIntegrationEventRecord}.
 * <p>
 * JPA entities have strict requirements:
 * <ul>
 *   <li>Must have a protected/public no-arg constructor for proxy creation</li>
 *   <li>Must not be final (for lazy loading proxies)</li>
 *   <li>Must have an {@code @Id} field</li>
 *   <li>Must have {@code @Entity} and {@code @Table} annotations</li>
 * </ul>
 * This test guards against accidental changes that would break JPA compatibility.
 */
class FailedIntegrationEventRecordJpaContractTest {

    @Test
    void entity_hasProtectedNoArgConstructor() {
        boolean hasProtectedNoArg = Arrays.stream(FailedIntegrationEventRecord.class.getDeclaredConstructors())
                .anyMatch(c -> c.getParameterCount() == 0 && Modifier.isProtected(c.getModifiers()));
        assertThat(hasProtectedNoArg)
                .as("JPA entities must have a protected or public no-arg constructor")
                .isTrue();
    }

    @Test
    void entity_noArgConstructor_producesNonNullInstance() throws Exception {
        Constructor<FailedIntegrationEventRecord> constructor =
                FailedIntegrationEventRecord.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        FailedIntegrationEventRecord instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }

    @Test
    void entity_noArgConstructor_leavesFieldsAsDefaults() throws Exception {
        Constructor<FailedIntegrationEventRecord> constructor =
                FailedIntegrationEventRecord.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        FailedIntegrationEventRecord instance = constructor.newInstance();

        assertThat(getField(instance, "id")).isNull();
        assertThat(getField(instance, "eventClassName")).isNull();
        assertThat(getField(instance, "eventPayload")).isNull();
        assertThat(getField(instance, "exceptionMessage")).isNull();
        assertThat(getField(instance, "exceptionClassName")).isNull();
        assertThat(getField(instance, "createdAt")).isNull();
        assertThat((int) getField(instance, "retryCount")).isZero();
        assertThat(getField(instance, "status")).isNull();
    }

    @Test
    void entity_isNotFinal() {
        assertThat(Modifier.isFinal(FailedIntegrationEventRecord.class.getModifiers()))
                .as("JPA entities must not be final — Hibernate may create proxy subclasses")
                .isFalse();
    }

    @Test
    void entity_isNotAbstract() {
        assertThat(Modifier.isAbstract(FailedIntegrationEventRecord.class.getModifiers()))
                .as("Concrete entity class must not be abstract")
                .isFalse();
    }

    @Test
    void entity_hasEntityAnnotation() {
        assertThat(FailedIntegrationEventRecord.class.getAnnotation(Entity.class)).isNotNull();
    }

    @Test
    void entity_hasTableAnnotation() {
        assertThat(FailedIntegrationEventRecord.class.getAnnotation(Table.class)).isNotNull();
    }

    @Test
    void entity_hasIdField() throws NoSuchFieldException {
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        assertThat(idField.getAnnotation(Id.class)).isNotNull();
    }

    @Test
    void entity_idField_hasGeneratedValueAnnotation() throws NoSuchFieldException {
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertThat(gv).isNotNull();
        assertThat(gv.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void entity_idField_isLongType() throws NoSuchFieldException {
        Field idField = FailedIntegrationEventRecord.class.getDeclaredField("id");
        assertThat(idField.getType()).isEqualTo(Long.class);
    }

    @Test
    void entity_statusField_hasEnumeratedAnnotation() throws NoSuchFieldException {
        Field statusField = FailedIntegrationEventRecord.class.getDeclaredField("status");
        Enumerated enumerated = statusField.getAnnotation(Enumerated.class);
        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void entity_hasTwoConstructors() {
        Constructor<?>[] constructors = FailedIntegrationEventRecord.class.getDeclaredConstructors();
        assertThat(constructors)
                .as("Entity should have exactly 2 constructors: protected no-arg (JPA) + public 5-arg (application)")
                .hasSize(2);
    }

    @Test
    void entity_publicConstructor_hasFiveParameters() {
        Constructor<?>[] publicConstructors = FailedIntegrationEventRecord.class.getConstructors();
        assertThat(publicConstructors).hasSize(1);
        assertThat(publicConstructors[0].getParameterCount()).isEqualTo(5);
    }

    @Test
    void entity_publicConstructor_parameterTypes() {
        Constructor<?>[] publicConstructors = FailedIntegrationEventRecord.class.getConstructors();
        Class<?>[] paramTypes = publicConstructors[0].getParameterTypes();
        assertThat(paramTypes[0]).isEqualTo(String.class); // eventClassName
        assertThat(paramTypes[1]).isEqualTo(String.class); // eventPayload
        assertThat(paramTypes[2]).isEqualTo(String.class); // exceptionMessage
        assertThat(paramTypes[3]).isEqualTo(String.class); // exceptionClassName
        assertThat(paramTypes[4]).isEqualTo(int.class);    // retryCount
    }

    @Test
    void entity_allFieldsArePrivate() {
        for (Field field : FailedIntegrationEventRecord.class.getDeclaredFields()) {
            assertThat(Modifier.isPrivate(field.getModifiers()))
                    .as("Field '%s' must be private for JPA entity encapsulation", field.getName())
                    .isTrue();
        }
    }

    @Test
    void entity_hasNoPublicSetters() {
        long setterCount = Arrays.stream(FailedIntegrationEventRecord.class.getDeclaredMethods())
                .filter(m -> m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers()))
                .count();
        assertThat(setterCount)
                .as("Entity should have no public setters — mutations should use domain methods only")
                .isZero();
    }

    @Test
    void entity_statusEnum_hasTwoValues() {
        assertThat(FailedIntegrationEventRecord.Status.values())
                .containsExactly(FailedIntegrationEventRecord.Status.FAILED, FailedIntegrationEventRecord.Status.RESOLVED);
    }

    @Test
    void entity_statusEnum_valueOfFailed() {
        assertThat(FailedIntegrationEventRecord.Status.valueOf("FAILED"))
                .isEqualTo(FailedIntegrationEventRecord.Status.FAILED);
    }

    @Test
    void entity_statusEnum_valueOfResolved() {
        assertThat(FailedIntegrationEventRecord.Status.valueOf("RESOLVED"))
                .isEqualTo(FailedIntegrationEventRecord.Status.RESOLVED);
    }

    private Object getField(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
