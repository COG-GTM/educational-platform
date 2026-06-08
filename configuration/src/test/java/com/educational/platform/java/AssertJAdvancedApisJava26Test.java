package com.educational.platform.java;

import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Validates AssertJ's advanced APIs — {@code SoftAssertions}, extracting,
 * filtering, {@code Condition}, exception assertions, and collection assertions
 * — on Java 26 compiled bytecode.
 * <p>
 * {@link AssertJMinVersionValidationTest} verifies the AssertJ version is >= 3.27.3
 * and that {@code SoftAssertions} can be instantiated. This test goes deeper by
 * exercising the advanced API surface that relies on Java reflection, lambda
 * proxying, and type inference on class file major version 70:
 * <ul>
 *   <li>{@code SoftAssertions} collecting multiple failures and reporting them</li>
 *   <li>{@code extracting()} with method references and lambdas</li>
 *   <li>{@code filteredOn()} predicates on Java 26 records</li>
 *   <li>{@code Condition} objects with custom predicates</li>
 *   <li>Exception assertion APIs ({@code assertThatThrownBy}, {@code catchThrowable})</li>
 *   <li>Map and Optional assertions</li>
 * </ul>
 */
public class AssertJAdvancedApisJava26Test {

    record CourseInfo(UUID id, String name, String status, int enrollmentCount) {}

    // --- SoftAssertions: collecting multiple checks ---

    @Test
    void softAssertions_shouldCollect_multipleChecks_onJava26() {
        var course = new CourseInfo(UUID.randomUUID(), "DDD Course", "PUBLISHED", 42);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(course.id()).isNotNull();
        soft.assertThat(course.name()).isEqualTo("DDD Course");
        soft.assertThat(course.status()).isEqualTo("PUBLISHED");
        soft.assertThat(course.enrollmentCount()).isGreaterThan(0);
        soft.assertAll();
    }

    @Test
    void softAssertions_assertSoftly_shouldWork_onJava26() {
        var course = new CourseInfo(UUID.randomUUID(), "CQRS Patterns", "DRAFT", 0);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(course.name()).contains("CQRS");
            soft.assertThat(course.status()).isIn("DRAFT", "PUBLISHED", "ARCHIVED");
            soft.assertThat(course.enrollmentCount()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void softAssertions_shouldDetectFailures_onJava26() {
        assertThatCode(() -> {
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(1).isEqualTo(2);
            soft.assertThat("a").isEqualTo("b");

            Throwable thrown = catchThrowable(soft::assertAll);
            assertThat(thrown)
                    .as("SoftAssertions should collect and report multiple failures")
                    .isNotNull()
                    .hasMessageContaining("1")
                    .hasMessageContaining("2");
        }).doesNotThrowAnyException();
    }

    // --- Extracting with method references ---

    @Test
    void extracting_withMethodRef_shouldWork_onJava26Records() {
        var courses = List.of(
                new CourseInfo(UUID.randomUUID(), "DDD", "PUBLISHED", 100),
                new CourseInfo(UUID.randomUUID(), "CQRS", "DRAFT", 0),
                new CourseInfo(UUID.randomUUID(), "Event Sourcing", "PUBLISHED", 50)
        );

        assertThat(courses)
                .extracting(CourseInfo::name)
                .containsExactly("DDD", "CQRS", "Event Sourcing");
    }

    @Test
    void extracting_multipleTuples_shouldWork_onJava26Records() {
        var courses = List.of(
                new CourseInfo(UUID.randomUUID(), "DDD", "PUBLISHED", 100),
                new CourseInfo(UUID.randomUUID(), "CQRS", "DRAFT", 0)
        );

        assertThat(courses)
                .extracting(CourseInfo::name, CourseInfo::status)
                .containsExactly(
                        tuple("DDD", "PUBLISHED"),
                        tuple("CQRS", "DRAFT")
                );
    }

    // --- Filtering ---

    @Test
    void filteredOn_withPredicate_shouldWork_onJava26Records() {
        var courses = List.of(
                new CourseInfo(UUID.randomUUID(), "DDD", "PUBLISHED", 100),
                new CourseInfo(UUID.randomUUID(), "CQRS", "DRAFT", 0),
                new CourseInfo(UUID.randomUUID(), "TDD", "PUBLISHED", 25)
        );

        assertThat(courses)
                .filteredOn(c -> "PUBLISHED".equals(c.status()))
                .hasSize(2)
                .extracting(CourseInfo::name)
                .containsExactlyInAnyOrder("DDD", "TDD");
    }

    @Test
    void filteredOn_withFieldName_shouldWork_onJava26Records() {
        var courses = List.of(
                new CourseInfo(UUID.randomUUID(), "A", "PUBLISHED", 100),
                new CourseInfo(UUID.randomUUID(), "B", "DRAFT", 0)
        );

        assertThat(courses)
                .filteredOn(c -> c.enrollmentCount() > 50)
                .hasSize(1)
                .first()
                .extracting(CourseInfo::name)
                .isEqualTo("A");
    }

    // --- Condition API ---

    @Test
    void condition_shouldEvaluate_onJava26Bytecode() {
        Condition<CourseInfo> published = new Condition<>(
                c -> "PUBLISHED".equals(c.status()),
                "published course");

        var course = new CourseInfo(UUID.randomUUID(), "DDD", "PUBLISHED", 100);

        assertThat(course).is(published);
    }

    @Test
    void condition_allOf_shouldWork_onJava26() {
        Condition<CourseInfo> hasEnrollments = new Condition<>(
                c -> c.enrollmentCount() > 0,
                "has enrollments");
        Condition<CourseInfo> published = new Condition<>(
                c -> "PUBLISHED".equals(c.status()),
                "published");

        var course = new CourseInfo(UUID.randomUUID(), "DDD", "PUBLISHED", 42);

        assertThat(course).is(org.assertj.core.api.Assertions.allOf(hasEnrollments, published));
    }

    // --- Exception assertions ---

    @Test
    void assertThatThrownBy_shouldWork_onJava26Bytecode() {
        assertThatThrownBy(() -> {
            throw new IllegalArgumentException("Java 26 exception test");
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Java 26 exception test")
                .hasNoCause();
    }

    @Test
    void catchThrowable_shouldCapture_onJava26Bytecode() {
        Throwable thrown = catchThrowable(() -> {
            throw new UnsupportedOperationException("not implemented");
        });

        assertThat(thrown)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not implemented");
    }

    // --- Map assertions ---

    @Test
    void mapAssertions_shouldWork_onJava26() {
        Map<String, String> versions = Map.of(
                "java", "26",
                "gradle", "9.5.1",
                "archunit", "1.4.2"
        );

        assertThat(versions)
                .hasSize(3)
                .containsEntry("java", "26")
                .containsKeys("java", "gradle", "archunit")
                .contains(entry("archunit", "1.4.2"));
    }

    // --- Optional assertions ---

    @Test
    void optionalAssertions_shouldWork_onJava26() {
        Optional<CourseInfo> present = Optional.of(
                new CourseInfo(UUID.randomUUID(), "DDD", "PUBLISHED", 10));

        assertThat(present)
                .isPresent()
                .hasValueSatisfying(c -> {
                    assertThat(c.name()).isEqualTo("DDD");
                    assertThat(c.status()).isEqualTo("PUBLISHED");
                });

        Optional<CourseInfo> empty = Optional.empty();
        assertThat(empty).isEmpty();
    }

    // --- Stream assertions ---

    @Test
    void streamAssertions_shouldWork_onJava26() {
        Stream<String> versions = Stream.of("Java 26", "Gradle 9.5.1", "ArchUnit 1.4.2");

        assertThat(versions)
                .hasSize(3)
                .anyMatch(s -> s.contains("26"))
                .allMatch(s -> !s.isBlank());
    }
}
