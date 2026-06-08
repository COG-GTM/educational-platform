package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates Java 21+ Sequenced Collections API on Java 26 bytecode.
 * <p>
 * Sequenced Collections (JEP 431) introduced {@link SequencedCollection},
 * {@link SequencedSet}, and {@link SequencedMap} with methods like
 * {@code getFirst()}, {@code getLast()}, and {@code reversed()}.
 * <p>
 * This test ensures these APIs compile and execute correctly under Java 26
 * bytecode (major version 70). The domain model benefits from ordered
 * collections (e.g., curriculum items in a course), making this API
 * relevant to the educational platform.
 */
public class Java26SequencedCollectionsTest {

    @Test
    void sequencedCollection_getFirst_getLast_shouldWork() {
        SequencedCollection<String> versions = new ArrayList<>(List.of("24", "25", "26"));

        assertThat(versions.getFirst())
                .as("getFirst() should return first element")
                .isEqualTo("24");

        assertThat(versions.getLast())
                .as("getLast() should return last element")
                .isEqualTo("26");
    }

    @Test
    void sequencedCollection_reversed_shouldReturnReverseView() {
        SequencedCollection<Integer> majorVersions = new ArrayList<>(List.of(68, 69, 70));

        SequencedCollection<Integer> reversed = majorVersions.reversed();

        assertThat(reversed.getFirst())
                .as("reversed().getFirst() should return the original last")
                .isEqualTo(70);

        assertThat(reversed.getLast())
                .as("reversed().getLast() should return the original first")
                .isEqualTo(68);
    }

    @Test
    void sequencedCollection_addFirst_addLast_shouldModifyOrder() {
        var items = new ArrayList<>(List.of("B", "C"));
        SequencedCollection<String> seq = items;

        seq.addFirst("A");
        seq.addLast("D");

        assertThat(seq)
                .as("addFirst/addLast should modify collection order")
                .containsExactly("A", "B", "C", "D");
    }

    @Test
    void sequencedCollection_removeFirst_removeLast_shouldWork() {
        var items = new ArrayList<>(List.of("Java 24", "Java 25", "Java 26"));
        SequencedCollection<String> seq = items;

        String first = seq.removeFirst();
        String last = seq.removeLast();

        assertThat(first).isEqualTo("Java 24");
        assertThat(last).isEqualTo("Java 26");
        assertThat(seq).containsExactly("Java 25");
    }

    @Test
    void sequencedSet_shouldMaintainInsertionOrder() {
        SequencedSet<String> deps = new LinkedHashSet<>();
        deps.add("junit-jupiter-api");
        deps.add("junit-jupiter-params");
        deps.add("assertj-core");

        assertThat(deps.getFirst()).isEqualTo("junit-jupiter-api");
        assertThat(deps.getLast()).isEqualTo("assertj-core");
    }

    @Test
    void sequencedMap_shouldSupportFirstLastEntry() {
        SequencedMap<String, String> versions = new LinkedHashMap<>();
        versions.put("archunit", "1.4.2");
        versions.put("mockito", "5.19.0");
        versions.put("assertj", "3.27.3");

        assertThat(versions.firstEntry().getKey()).isEqualTo("archunit");
        assertThat(versions.lastEntry().getValue()).isEqualTo("3.27.3");
    }

    @Test
    void sequencedMap_reversed_shouldReverseEntryOrder() {
        SequencedMap<String, String> map = new LinkedHashMap<>();
        map.put("source", "VERSION_26");
        map.put("target", "VERSION_26");

        SequencedMap<String, String> reversed = map.reversed();

        assertThat(reversed.firstEntry().getKey())
                .as("reversed map should have 'target' first")
                .isEqualTo("target");
    }

    @Test
    void sequencedMap_pollFirstEntry_pollLastEntry_shouldWork() {
        SequencedMap<Integer, String> javaVersions = new LinkedHashMap<>();
        javaVersions.put(24, "Gradle 9.0");
        javaVersions.put(25, "Gradle 9.2");
        javaVersions.put(26, "Gradle 9.4");

        var first = javaVersions.pollFirstEntry();
        var last = javaVersions.pollLastEntry();

        assertThat(first.getKey()).isEqualTo(24);
        assertThat(last.getValue()).isEqualTo("Gradle 9.4");
        assertThat(javaVersions).hasSize(1).containsKey(25);
    }

    @Test
    void emptySequencedCollection_getFirst_shouldThrow() {
        SequencedCollection<String> empty = new ArrayList<>();

        assertThatThrownBy(empty::getFirst)
                .as("getFirst() on empty collection should throw")
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void emptySequencedCollection_getLast_shouldThrow() {
        SequencedCollection<String> empty = new ArrayList<>();

        assertThatThrownBy(empty::getLast)
                .as("getLast() on empty collection should throw")
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    static Stream<Arguments> collectionImplementations() {
        return Stream.of(
                Arguments.of("ArrayList", new ArrayList<>(List.of("a", "b", "c"))),
                Arguments.of("LinkedHashSet", new LinkedHashSet<>(List.of("a", "b", "c")))
        );
    }

    @ParameterizedTest(name = "{0} should implement SequencedCollection correctly on Java 26")
    @MethodSource("collectionImplementations")
    void collectionImplementation_shouldSupportSequencedOps(
            String implName, SequencedCollection<String> collection) {

        assertThatCode(() -> {
            assertThat(collection.getFirst()).isEqualTo("a");
            assertThat(collection.getLast()).isEqualTo("c");
            assertThat(collection.reversed().getFirst()).isEqualTo("c");
        }).as("%s should support SequencedCollection operations on Java 26 bytecode", implName)
                .doesNotThrowAnyException();
    }
}
