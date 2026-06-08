package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validates that Mockito's byte-buddy proxy generation works correctly for
 * JPA repository interfaces and Spring-managed classes compiled with Java 26.
 * <p>
 * Mockito uses byte-buddy to generate subclass proxies at runtime. Byte-buddy
 * must parse Java 26 class files (major version 70) to create mocks and spies.
 * {@link TestFrameworkStackJava26Test} covers basic mock/spy functionality;
 * this test targets the JPA repository and domain-entity proxy paths that
 * Hibernate and Spring Data exercise at runtime.
 */
public class MockitoJpaProxyJava26Test {

    // --- Mock JPA-style repository interfaces compiled with Java 26 ---

    interface EntityRepository<T> {
        Optional<T> findById(Long id);
        Optional<T> findByUuid(UUID uuid);
        T save(T entity);
        List<T> findAll();
        void deleteById(Long id);
    }

    interface CourseRepository extends EntityRepository<Object> {
        Optional<Object> findByName(String name);
    }

    @Test
    void mockito_shouldCreateProxy_forGenericRepositoryInterface() {
        @SuppressWarnings("unchecked")
        EntityRepository<Object> repo = mock(EntityRepository.class);
        UUID testUuid = UUID.randomUUID();

        when(repo.findByUuid(testUuid)).thenReturn(Optional.of("mock-entity"));
        when(repo.findAll()).thenReturn(List.of("a", "b"));

        assertThat(repo.findByUuid(testUuid)).contains("mock-entity");
        assertThat(repo.findAll()).hasSize(2);
        verify(repo).findByUuid(testUuid);
    }

    @Test
    void mockito_shouldCreateProxy_forDerivedRepositoryInterface() {
        CourseRepository repo = mock(CourseRepository.class);
        when(repo.findByName("DDD Course")).thenReturn(Optional.of("course-1"));
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(repo.findByName("DDD Course")).isPresent();
        Object saved = repo.save("new-course");
        assertThat(saved).isEqualTo("new-course");
    }

    @Test
    void mockito_shouldMockVoidMethod_onRepositoryInterface() {
        CourseRepository repo = mock(CourseRepository.class);

        assertThatCode(() -> repo.deleteById(1L))
                .doesNotThrowAnyException();
        verify(repo).deleteById(1L);
    }

    // --- Mock abstract domain classes (Hibernate proxy pattern) ---

    static abstract class AbstractEntity {
        private Long id;
        private UUID uuid;

        protected AbstractEntity() {}

        Long getId() { return id; }
        UUID getUuid() { return uuid; }
        abstract String getDomainType();
    }

    @Test
    void mockito_shouldCreateProxy_forAbstractEntityClass() {
        AbstractEntity entity = mock(AbstractEntity.class);
        UUID testUuid = UUID.randomUUID();

        when(entity.getUuid()).thenReturn(testUuid);
        when(entity.getDomainType()).thenReturn("Course");

        assertThat(entity.getUuid()).isEqualTo(testUuid);
        assertThat(entity.getDomainType()).isEqualTo("Course");
    }

    // --- Spy on concrete domain-like classes ---

    static class DomainEvent {
        private final UUID entityId;
        private final String eventType;

        DomainEvent(UUID entityId, String eventType) {
            this.entityId = entityId;
            this.eventType = eventType;
        }

        UUID getEntityId() { return entityId; }
        String getEventType() { return eventType; }
    }

    @Test
    void mockito_shouldSpy_onDomainEventClass() {
        UUID id = UUID.randomUUID();
        DomainEvent event = new DomainEvent(id, "CourseCreated");
        DomainEvent spy = Mockito.spy(event);

        assertThat(spy.getEntityId()).isEqualTo(id);
        assertThat(spy.getEventType()).isEqualTo("CourseCreated");
        verify(spy).getEventType();
    }

    // --- Verify actual production repository interfaces are mockable ---

    @ParameterizedTest(name = "Production repository interface should be mockable: {0}")
    @ValueSource(strings = {
            "com.educational.platform.courses.course.CourseRepository",
            "com.educational.platform.administration.course.CourseProposalRepository",
            "com.educational.platform.course.enrollments.CourseEnrollmentRepository",
            "com.educational.platform.course.reviews.CourseReviewRepository",
            "com.educational.platform.users.UserRepository"
    })
    void productionRepository_shouldBeMockable_onJava26(String className) {
        assertThatCode(() -> {
            Class<?> repoClass = Class.forName(className);
            assertThat(repoClass.isInterface() || Modifier.isAbstract(repoClass.getModifiers()))
                    .as("Repository '%s' should be an interface or abstract (mockable)", className)
                    .isTrue();

            Object mockRepo = mock(repoClass);
            assertThat(mockRepo).isNotNull();
        }).as("Mockito should create a proxy for '%s' compiled with Java 26", className)
                .doesNotThrowAnyException();
    }

    // --- Mockito argument matchers with Java 26 generics ---

    @Test
    void mockito_argumentMatchers_shouldWork_withJava26Generics() {
        @SuppressWarnings("unchecked")
        EntityRepository<String> repo = mock(EntityRepository.class);
        when(repo.save(any())).thenReturn("saved");

        String result = repo.save("test-entity");
        assertThat(result).isEqualTo("saved");
        verify(repo).save(any());
    }
}
