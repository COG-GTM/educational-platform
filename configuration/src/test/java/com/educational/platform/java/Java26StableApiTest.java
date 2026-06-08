package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that Java 26 stable APIs (finalized in Java 22–26) are available
 * and functional at runtime. These APIs were preview or incubating in earlier
 * versions and are now permanent parts of the platform.
 * <p>
 * If any of these tests fail, the runtime is not a genuine Java 26 JVM or
 * the toolchain is misconfigured.
 */
public class Java26StableApiTest {

    // --- Stream Gatherers (JEP 485, finalized in Java 24) ---

    @Test
    void streamGatherers_windowFixed_shouldPartitionElements() {
        List<List<Integer>> windows = List.of(1, 2, 3, 4, 5).stream()
                .gather(Gatherers.windowFixed(2))
                .toList();

        assertThat(windows)
                .as("windowFixed(2) should partition into windows of size 2")
                .hasSize(3)
                .containsExactly(
                        List.of(1, 2),
                        List.of(3, 4),
                        List.of(5)
                );
    }

    @Test
    void streamGatherers_windowFixed_singleElement() {
        List<List<String>> windows = List.of("Java26").stream()
                .gather(Gatherers.windowFixed(3))
                .toList();

        assertThat(windows)
                .as("Single element smaller than window should produce one partial window")
                .hasSize(1)
                .containsExactly(List.of("Java26"));
    }

    @Test
    void streamGatherers_windowFixed_emptyStream() {
        List<List<Object>> windows = Stream.empty()
                .gather(Gatherers.windowFixed(5))
                .toList();

        assertThat(windows)
                .as("Empty stream should produce no windows")
                .isEmpty();
    }

    @Test
    void streamGatherers_windowSliding_shouldCreateOverlappingWindows() {
        List<List<Integer>> windows = List.of(1, 2, 3, 4).stream()
                .gather(Gatherers.windowSliding(3))
                .toList();

        assertThat(windows)
                .as("windowSliding(3) should create overlapping windows")
                .hasSize(2)
                .containsExactly(
                        List.of(1, 2, 3),
                        List.of(2, 3, 4)
                );
    }

    @Test
    void streamGatherers_fold_shouldAccumulateValues() {
        int sum = List.of(1, 2, 3, 4, 5).stream()
                .gather(Gatherers.fold(() -> 0, Integer::sum))
                .findFirst()
                .orElse(0);

        assertThat(sum)
                .as("fold should accumulate all values")
                .isEqualTo(15);
    }

    @Test
    void streamGatherers_scan_shouldProduceRunningTotals() {
        List<Integer> runningTotals = List.of(1, 2, 3, 4).stream()
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();

        assertThat(runningTotals)
                .as("scan should produce running totals")
                .containsExactly(1, 3, 6, 10);
    }

    @Test
    void streamGatherers_mapConcurrent_shouldTransformElements() {
        List<String> result = List.of(1, 2, 3).stream()
                .gather(Gatherers.mapConcurrent(2, i -> "v" + i))
                .toList();

        assertThat(result)
                .as("mapConcurrent should transform all elements")
                .hasSize(3)
                .allMatch(s -> s.startsWith("v"));
    }

    // --- Foreign Function & Memory API (JEP 454, finalized in Java 22) ---

    @Test
    void foreignMemory_nullSegment_shouldBeAccessible() {
        MemorySegment nullSegment = MemorySegment.NULL;

        assertThat(nullSegment)
                .as("MemorySegment.NULL should be available")
                .isNotNull();

        assertThat(nullSegment.byteSize())
                .as("NULL segment should have zero byte size")
                .isZero();
    }

    @Test
    void foreignMemory_arena_shouldAllocateAndDeallocate() {
        assertThatCode(() -> {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment segment = arena.allocate(ValueLayout.JAVA_INT);
                segment.set(ValueLayout.JAVA_INT, 0, 42);
                int value = segment.get(ValueLayout.JAVA_INT, 0);
                assertThat(value).isEqualTo(42);
            }
        }).as("Arena allocation and access should work on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void foreignMemory_arena_shouldAllocateArray() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(ValueLayout.JAVA_LONG, 4);

            for (int i = 0; i < 4; i++) {
                segment.setAtIndex(ValueLayout.JAVA_LONG, i, (long) i * 10);
            }

            assertThat(segment.getAtIndex(ValueLayout.JAVA_LONG, 0)).isEqualTo(0L);
            assertThat(segment.getAtIndex(ValueLayout.JAVA_LONG, 3)).isEqualTo(30L);
        }
    }

    // --- Class-File API (JEP 484, finalized in Java 24) ---

    @ParameterizedTest(name = "Class-File API class should be loadable: {0}")
    @ValueSource(strings = {
            "java.lang.classfile.ClassFile",
            "java.lang.classfile.ClassModel",
            "java.lang.classfile.MethodModel",
            "java.lang.classfile.FieldModel",
            "java.lang.classfile.attribute.CodeAttribute"
    })
    void classFileApi_class_shouldBeLoadable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("Class-File API class '%s' should be available on Java 26", className)
                .doesNotThrowAnyException();
    }

    @Test
    void classFileApi_shouldParse_thisTestClass() {
        assertThatCode(() -> {
            var classFile = java.lang.classfile.ClassFile.of();
            byte[] bytes = getClass().getResourceAsStream(
                    "/" + getClass().getName().replace('.', '/') + ".class"
            ).readAllBytes();

            var classModel = classFile.parse(bytes);

            assertThat(classModel.majorVersion())
                    .as("Parsed class major version should be 70 (Java 26)")
                    .isEqualTo(70);

            assertThat(classModel.thisClass().asInternalName())
                    .as("Parsed class name should match this test class")
                    .isEqualTo("com/educational/platform/java/Java26StableApiTest");
        }).doesNotThrowAnyException();
    }

    @Test
    void classFileApi_shouldReadMethods() {
        assertThatCode(() -> {
            var classFile = java.lang.classfile.ClassFile.of();
            byte[] bytes = getClass().getResourceAsStream(
                    "/" + getClass().getName().replace('.', '/') + ".class"
            ).readAllBytes();

            var classModel = classFile.parse(bytes);
            var methods = classModel.methods();

            assertThat(methods)
                    .as("Class-File API should find methods in this test class")
                    .isNotEmpty();
        }).doesNotThrowAnyException();
    }

    // --- Sequenced Collections (JEP 431, finalized in Java 21, stable by 26) ---

    @Test
    void sequencedCollection_reversed_shouldBeAvailable() {
        var list = List.of("Java", "26", "Gradle", "9.5.1");

        var reversed = list.reversed();

        assertThat(reversed)
                .as("SequencedCollection.reversed() should work on Java 26")
                .containsExactly("9.5.1", "Gradle", "26", "Java");
    }

    @Test
    void sequencedCollection_getFirst_getLast() {
        var list = List.of("ArchUnit", "1.4.2");

        assertThat(list.getFirst()).isEqualTo("ArchUnit");
        assertThat(list.getLast()).isEqualTo("1.4.2");
    }
}
