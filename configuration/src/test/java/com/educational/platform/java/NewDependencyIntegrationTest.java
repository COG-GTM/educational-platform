package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test exercising all three new test dependencies added by this PR
 * working together in a single test class on Java 26:
 * <ul>
 *   <li>{@code junit-jupiter-params} — parameterized tests</li>
 *   <li>{@code assertj-core} — fluent assertions</li>
 *   <li>{@code junit-jupiter-engine} — JUnit 5 test engine (implicit via test execution)</li>
 * </ul>
 * Combined with Mockito (pre-existing) to verify no classpath conflicts.
 * <p>
 * Individual dependency availability is tested by {@link NewTestDependencyClasspathTest}.
 * This test verifies these dependencies function <em>together</em> in realistic
 * test scenarios compiled with Java 26 bytecode.
 */
public class NewDependencyIntegrationTest {

    // --- Parameterized tests with AssertJ assertions ---

    @ParameterizedTest(name = "Java version {0} requires class file major version {1}")
    @CsvSource({
            "26, 70",
            "25, 69",
            "21, 65",
            "17, 61",
            "11, 55",
            "8, 52"
    })
    void classFileMajorVersion_formula(int javaVersion, int expectedMajor) {
        int computedMajor = 44 + javaVersion;

        assertThat(computedMajor)
                .as("Java %d → major version %d", javaVersion, expectedMajor)
                .isEqualTo(expectedMajor);
    }

    @ParameterizedTest(name = "Gradle {0} should support Java 26: {1}")
    @CsvSource({
            "9.5.1, true",
            "9.4.0, true",
            "9.3.9, false",
            "9.2.1, false",
            "10.0.0, true"
    })
    void gradleCompatibility_withAssertJ(String gradleVersion, boolean expectedSupport) {
        String[] parts = gradleVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);

        boolean supports = major > 9 || (major == 9 && minor >= 4);

        assertThat(supports)
                .as("Gradle %s Java 26 support", gradleVersion)
                .isEqualTo(expectedSupport);
    }

    // --- @MethodSource with AssertJ and Mockito ---

    interface CourseService {
        Optional<String> findCourseByName(String name);
        List<String> listCourses();
        void enrollStudent(String courseId, String studentId);
    }

    static Stream<Arguments> courseNameScenarios() {
        return Stream.of(
                Arguments.of("DDD Fundamentals", true),
                Arguments.of("", false),
                Arguments.of("   ", false),
                Arguments.of(null, false)
        );
    }

    @ParameterizedTest(name = "Course name ''{0}'' valid: {1}")
    @MethodSource("courseNameScenarios")
    void mockService_withParameterizedInput_andAssertJ(String courseName, boolean shouldFind) {
        CourseService service = mock(CourseService.class);

        if (shouldFind) {
            when(service.findCourseByName(courseName)).thenReturn(Optional.of("course-123"));
        } else {
            when(service.findCourseByName(courseName)).thenReturn(Optional.empty());
        }

        Optional<String> result = service.findCourseByName(courseName);

        if (shouldFind) {
            assertThat(result)
                    .as("Valid course name '%s' should return a result", courseName)
                    .isPresent()
                    .contains("course-123");
        } else {
            assertThat(result)
                    .as("Invalid course name '%s' should return empty", courseName)
                    .isEmpty();
        }

        verify(service).findCourseByName(courseName);
    }

    // --- @ValueSource with Mockito and AssertJ ---

    @ParameterizedTest(name = "Course list size: {0}")
    @ValueSource(ints = {0, 1, 5, 100})
    void mockService_shouldReturn_variableListSizes(int size) {
        CourseService service = mock(CourseService.class);
        List<String> courses = Stream.generate(() -> "course")
                .limit(size)
                .toList();

        when(service.listCourses()).thenReturn(courses);

        assertThat(service.listCourses())
                .hasSize(size);
    }

    // --- Verify all frameworks work with Java 26 sealed types ---

    sealed interface DependencyScope permits CompileScope, RuntimeScope {}
    record CompileScope(String artifact) implements DependencyScope {}
    record RuntimeScope(String artifact) implements DependencyScope {}

    static Stream<Arguments> dependencyScopes() {
        return Stream.of(
                Arguments.of(new CompileScope("junit-jupiter-params"), "testImplementation"),
                Arguments.of(new CompileScope("assertj-core"), "testImplementation"),
                Arguments.of(new RuntimeScope("junit-jupiter-engine"), "testRuntimeOnly")
        );
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("dependencyScopes")
    void sealedTypes_shouldWork_inParameterizedTests(DependencyScope dep, String expectedScope) {
        String actualScope = switch (dep) {
            case CompileScope c -> "testImplementation";
            case RuntimeScope r -> "testRuntimeOnly";
        };

        assertThat(actualScope).isEqualTo(expectedScope);
    }

    // --- Exception testing with all dependencies ---

    @Test
    void assertThatThrownBy_shouldWork_withMockitoAndAssertJ_onJava26() {
        CourseService service = mock(CourseService.class);
        Mockito.doThrow(new IllegalStateException("Course is full"))
                .when(service).enrollStudent("course-1", "student-1");

        assertThatThrownBy(() -> service.enrollStudent("course-1", "student-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Course is full");
    }

    @Test
    void softAssertions_withMockitoVerification_onJava26() {
        CourseService service = mock(CourseService.class);
        when(service.listCourses()).thenReturn(List.of("DDD", "CQRS", "Event Sourcing"));
        when(service.findCourseByName("DDD")).thenReturn(Optional.of("ddd-101"));

        var courses = service.listCourses();
        var found = service.findCourseByName("DDD");

        org.assertj.core.api.SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(courses).hasSize(3);
            soft.assertThat(courses).contains("DDD");
            soft.assertThat(found).isPresent();
        });

        verify(service).listCourses();
        verify(service).findCourseByName("DDD");
    }

    // --- Verify JUnit engine is running these tests ---

    @Test
    void junitEngine_shouldExecute_thisTestClass() {
        assertThatCode(() ->
                Class.forName("org.junit.jupiter.engine.JupiterTestEngine")
        ).as("JupiterTestEngine must be discoverable to run this test")
                .doesNotThrowAnyException();
    }
}
