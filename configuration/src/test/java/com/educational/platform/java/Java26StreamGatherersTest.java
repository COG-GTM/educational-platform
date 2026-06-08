package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Stream Gatherers API (JEP 485, finalized in Java 24)
 * works correctly under Java 26.
 * <p>
 * Stream Gatherers extend the Stream API with custom intermediate operations
 * via {@link java.util.stream.Gatherers}. This API was preview in Java 22–23
 * and became final in Java 24. Since the project upgraded to Java 26, these
 * APIs should be fully available without any preview flags.
 * <p>
 * The existing {@link Java26LanguageFeaturesTest} tests basic stream operations
 * (e.g., {@code Stream.toList()}). This test specifically exercises the
 * <em>Gatherers</em> API, which is a more recent addition that validates the
 * Java 26 standard library is correctly resolved.
 */
public class Java26StreamGatherersTest {

    @Test
    void gatherers_windowFixed_shouldPartition_intoFixedSizeGroups() {
        List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5, 6)
                .gather(Gatherers.windowFixed(3))
                .toList();

        assertThat(windows)
                .hasSize(2)
                .containsExactly(
                        List.of(1, 2, 3),
                        List.of(4, 5, 6)
                );
    }

    @Test
    void gatherers_windowFixed_shouldHandle_incompleteLastWindow() {
        List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
                .gather(Gatherers.windowFixed(3))
                .toList();

        assertThat(windows)
                .hasSize(2)
                .containsExactly(
                        List.of(1, 2, 3),
                        List.of(4, 5)
                );
    }

    @Test
    void gatherers_windowFixed_shouldHandle_emptyStream() {
        List<List<Integer>> windows = Stream.<Integer>empty()
                .gather(Gatherers.windowFixed(3))
                .toList();

        assertThat(windows).isEmpty();
    }

    @Test
    void gatherers_windowSliding_shouldCreateOverlappingWindows() {
        List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
                .gather(Gatherers.windowSliding(3))
                .toList();

        assertThat(windows)
                .hasSize(3)
                .containsExactly(
                        List.of(1, 2, 3),
                        List.of(2, 3, 4),
                        List.of(3, 4, 5)
                );
    }

    @Test
    void gatherers_windowSliding_shouldHandle_windowLargerThanStream() {
        List<List<Integer>> windows = Stream.of(1, 2)
                .gather(Gatherers.windowSliding(5))
                .toList();

        assertThat(windows)
                .hasSize(1)
                .containsExactly(List.of(1, 2));
    }

    @Test
    void gatherers_fold_shouldAccumulate_values() {
        // fold is a terminal-style gatherer that produces a single result
        List<String> result = Stream.of("Java", " ", "26")
                .gather(Gatherers.fold(
                        () -> new StringBuilder(),
                        (sb, s) -> { sb.append(s); return sb; }
                ))
                .map(StringBuilder::toString)
                .toList();

        assertThat(result)
                .hasSize(1)
                .containsExactly("Java 26");
    }

    @Test
    void gatherers_scan_shouldEmit_runningAccumulations() {
        List<Integer> runningSum = Stream.of(1, 2, 3, 4, 5)
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();

        assertThat(runningSum)
                .containsExactly(1, 3, 6, 10, 15);
    }

    @Test
    void gatherers_scan_shouldWork_withStrings() {
        List<String> accumulated = Stream.of("a", "b", "c")
                .gather(Gatherers.scan(() -> "", (acc, s) -> acc + s))
                .toList();

        assertThat(accumulated)
                .containsExactly("a", "ab", "abc");
    }

    @Test
    void gatherers_mapConcurrent_shouldTransform_elements() {
        List<String> results = Stream.of(9, 5, 1)
                .gather(Gatherers.mapConcurrent(4, v -> "v" + v))
                .toList();

        assertThat(results)
                .hasSize(3)
                .containsExactlyInAnyOrder("v9", "v5", "v1");
    }

    @Test
    void gatherers_shouldBeChainable_withStandardStreamOps() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .gather(Gatherers.windowFixed(2))
                .map(window -> window.stream().mapToInt(Integer::intValue).sum())
                .filter(sum -> sum > 5)
                .toList();

        // Windows: [1,2]=3, [3,4]=7, [5,6]=11, [7,8]=15, [9,10]=19
        // Filter > 5: 7, 11, 15, 19
        assertThat(result).containsExactly(7, 11, 15, 19);
    }

    @Test
    void gatherersClass_shouldBeAvailable_onJava26Classpath() {
        assertThatCode(() -> Class.forName("java.util.stream.Gatherers"))
                .as("Gatherers class should be available on Java 26 (finalized in Java 24)")
                .doesNotThrowAnyException();
    }

    @Test
    void gathererInterface_shouldBeAvailable_onJava26Classpath() {
        assertThatCode(() -> Class.forName("java.util.stream.Gatherer"))
                .as("Gatherer interface should be available on Java 26")
                .doesNotThrowAnyException();
    }
}
