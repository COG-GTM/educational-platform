package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validates that Mockito's annotation-based injection ({@code @Mock},
 * {@code @InjectMocks}, {@code @Spy}) works correctly on Java 26 bytecode.
 * <p>
 * {@link MockitoJpaProxyJava26Test} validates proxy generation for
 * repository interfaces and domain classes using {@code Mockito.mock()}
 * and {@code Mockito.spy()} programmatically. This test exercises the
 * <em>annotation-driven</em> injection path via {@link MockitoExtension},
 * which uses reflection to scan fields, inject mocks, and wire
 * dependencies — a distinct code path that relies on Java 26 class file
 * metadata being correctly readable by Mockito's internal reflector.
 * <p>
 * {@code @InjectMocks} performs constructor or setter injection by
 * inspecting the target class's constructors and fields. On Java 26
 * bytecode, the constructor parameter metadata and field type descriptors
 * use class file format version 70, which Mockito's byte-buddy and
 * reflection layer must handle.
 */
@ExtendWith(MockitoExtension.class)
public class MockitoAnnotationInjectionJava26Test {

    // --- Domain collaborators ---

    interface CourseRepository {
        Optional<CourseDTO> findByUuid(UUID uuid);
        CourseDTO save(CourseDTO course);
        List<CourseDTO> findAll();
    }

    interface EventPublisher {
        void publish(String eventType, UUID entityId);
    }

    record CourseDTO(UUID uuid, String name, String status) {}

    // --- Service under test ---

    static class CourseService {
        private final CourseRepository repository;
        private final EventPublisher eventPublisher;

        CourseService(CourseRepository repository, EventPublisher eventPublisher) {
            this.repository = repository;
            this.eventPublisher = eventPublisher;
        }

        Optional<CourseDTO> findCourse(UUID uuid) {
            return repository.findByUuid(uuid);
        }

        CourseDTO createCourse(String name) {
            var course = new CourseDTO(UUID.randomUUID(), name, "DRAFT");
            var saved = repository.save(course);
            eventPublisher.publish("CourseCreated", saved.uuid());
            return saved;
        }

        List<CourseDTO> listAll() {
            return repository.findAll();
        }
    }

    // --- Mockito annotation fields ---

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CourseService courseService;

    // --- @Mock injection tests ---

    @Test
    void mock_shouldBeInjected_byMockitoExtension() {
        assertThat(courseRepository)
                .as("@Mock CourseRepository should be injected on Java 26")
                .isNotNull();
        assertThat(eventPublisher)
                .as("@Mock EventPublisher should be injected on Java 26")
                .isNotNull();
    }

    // --- @InjectMocks constructor injection ---

    @Test
    void injectMocks_shouldWire_allDependencies() {
        assertThat(courseService)
                .as("@InjectMocks CourseService should be constructed on Java 26")
                .isNotNull();
    }

    @Test
    void injectMocks_service_shouldUse_injectedMock() {
        UUID uuid = UUID.randomUUID();
        var expected = new CourseDTO(uuid, "DDD Course", "PUBLISHED");
        when(courseRepository.findByUuid(uuid)).thenReturn(Optional.of(expected));

        Optional<CourseDTO> result = courseService.findCourse(uuid);

        assertThat(result)
                .as("Service should return the mock's response")
                .isPresent()
                .contains(expected);
        verify(courseRepository).findByUuid(uuid);
    }

    @Test
    void injectMocks_service_shouldInvoke_bothCollaborators() {
        var course = new CourseDTO(UUID.randomUUID(), "Test Course", "DRAFT");
        when(courseRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(course);

        CourseDTO result = courseService.createCourse("Test Course");

        assertThat(result.name()).isEqualTo("Test Course");
        verify(courseRepository).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher).publish(
                org.mockito.ArgumentMatchers.eq("CourseCreated"),
                org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void injectMocks_service_listAll_shouldDelegateToRepository() {
        var courses = List.of(
                new CourseDTO(UUID.randomUUID(), "Course A", "PUBLISHED"),
                new CourseDTO(UUID.randomUUID(), "Course B", "DRAFT")
        );
        when(courseRepository.findAll()).thenReturn(courses);

        List<CourseDTO> result = courseService.listAll();

        assertThat(result).hasSize(2);
        verify(courseRepository).findAll();
    }

    // --- @Spy tests ---

    static class AuditLogger {
        private int logCount = 0;

        String log(String message) {
            logCount++;
            return "[AUDIT] " + message;
        }

        int getLogCount() {
            return logCount;
        }
    }

    @Spy
    private AuditLogger auditLogger = new AuditLogger();

    @Test
    void spy_shouldBeInjected_byMockitoExtension() {
        assertThat(auditLogger)
                .as("@Spy AuditLogger should be injected on Java 26")
                .isNotNull();
    }

    @Test
    void spy_shouldCallRealMethod_byDefault() {
        String result = auditLogger.log("Course created");

        assertThat(result)
                .as("Spy should call real method on Java 26 bytecode")
                .isEqualTo("[AUDIT] Course created");
        assertThat(auditLogger.getLogCount()).isEqualTo(1);
        verify(auditLogger).log("Course created");
    }

    @Test
    void spy_shouldAllowStubbing_overRealMethod() {
        when(auditLogger.log("stubbed")).thenReturn("[STUB]");

        assertThat(auditLogger.log("stubbed")).isEqualTo("[STUB]");
        assertThat(auditLogger.log("real")).isEqualTo("[AUDIT] real");
    }

    // --- Annotation retention verification ---

    @Test
    void mockAnnotation_shouldBe_retainedAtRuntime() {
        long mockFields = java.util.Arrays.stream(
                        MockitoAnnotationInjectionJava26Test.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Mock.class))
                .count();

        assertThat(mockFields)
                .as("@Mock fields should be discoverable via reflection on Java 26")
                .isEqualTo(2);
    }

    @Test
    void injectMocksAnnotation_shouldBe_retainedAtRuntime() {
        long injectMocksFields = java.util.Arrays.stream(
                        MockitoAnnotationInjectionJava26Test.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(InjectMocks.class))
                .count();

        assertThat(injectMocksFields)
                .as("@InjectMocks fields should be discoverable via reflection on Java 26")
                .isEqualTo(1);
    }

    @Test
    void spyAnnotation_shouldBe_retainedAtRuntime() {
        long spyFields = java.util.Arrays.stream(
                        MockitoAnnotationInjectionJava26Test.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Spy.class))
                .count();

        assertThat(spyFields)
                .as("@Spy fields should be discoverable via reflection on Java 26")
                .isEqualTo(1);
    }
}
