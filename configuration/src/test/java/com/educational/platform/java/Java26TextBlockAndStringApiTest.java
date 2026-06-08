package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that Java text blocks, modern String APIs, and Stream.toList()
 * work correctly when compiled and run on Java 26.
 * <p>
 * {@link Java26LanguageFeaturesTest} covers records, sealed classes, pattern
 * matching, and unnamed variables. This test exercises the text block syntax
 * (Java 13+), modern String methods, and Stream terminal operations that
 * are commonly used throughout the project but were not directly validated
 * for Java 26 bytecode compilation.
 */
public class Java26TextBlockAndStringApiTest {

    // --- Text blocks (Java 13+) ---

    @Test
    void textBlock_shouldPreserve_multilineContent() {
        String json = """
                {
                    "name": "DDD Course",
                    "version": 26
                }
                """;

        assertThat(json)
                .contains("\"name\": \"DDD Course\"")
                .contains("\"version\": 26")
                .contains("\n");
    }

    @Test
    void textBlock_shouldStripIndentation_correctly() {
        String text = """
                line1
                line2
                line3
                """;

        assertThat(text.lines().count())
                .as("Text block should produce 3 content lines")
                .isEqualTo(3);

        assertThat(text.lines().filter(l -> !l.isEmpty()).count())
                .isEqualTo(3);
    }

    @Test
    void textBlock_withEmbeddedQuotes_shouldCompile() {
        String sql = """
                SELECT * FROM courses
                WHERE name = 'DDD Course'
                AND status = "APPROVED"
                """;

        assertThat(sql)
                .contains("SELECT")
                .contains("'DDD Course'")
                .contains("\"APPROVED\"");
    }

    // --- String API methods ---

    @Test
    void stringIsBlank_shouldWorkOnJava26() {
        assertThat("".isBlank()).isTrue();
        assertThat("   ".isBlank()).isTrue();
        assertThat("\t\n".isBlank()).isTrue();
        assertThat("hello".isBlank()).isFalse();
    }

    @Test
    void stringStrip_shouldWorkOnJava26() {
        String padded = "  hello  ";
        assertThat(padded.strip()).isEqualTo("hello");
        assertThat(padded.stripLeading()).isEqualTo("hello  ");
        assertThat(padded.stripTrailing()).isEqualTo("  hello");
    }

    @Test
    void stringRepeat_shouldWorkOnJava26() {
        assertThat("ab".repeat(3)).isEqualTo("ababab");
        assertThat("x".repeat(0)).isEmpty();
    }

    @Test
    void stringLines_shouldSplitByLineTerminator() {
        String multiline = "line1\nline2\rline3\r\nline4";
        List<String> lines = multiline.lines().toList();

        assertThat(lines).containsExactly("line1", "line2", "line3", "line4");
    }

    @ParameterizedTest(name = "String.indent({0}) should adjust indentation")
    @CsvSource({"2", "4", "0"})
    void stringIndent_shouldAdjustIndentation(int spaces) {
        String original = "hello";
        String indented = original.indent(spaces);

        assertThat(indented)
                .as("indent(%d) should add %d leading spaces", spaces, spaces)
                .startsWith(" ".repeat(spaces) + "hello");
    }

    // --- Stream.toList() (Java 16+) ---

    @Test
    void streamToList_shouldReturn_unmodifiableList() {
        List<String> result = Stream.of("a", "b", "c").toList();

        assertThat(result)
                .containsExactly("a", "b", "c");

        assertThatThrownBy(() -> result.add("d"))
                .as("Stream.toList() should return unmodifiable list")
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void streamCollectToList_shouldReturn_modifiableList() {
        List<String> result = Stream.of("a", "b", "c")
                .collect(Collectors.toList());

        result.add("d");
        assertThat(result).hasSize(4);
    }

    // --- String edge cases ---

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "   \n  "})
    void blankDetection_shouldHandle_edgeCases(String input) {
        if (input == null) {
            assertThat(input).isNull();
        } else {
            assertThat(input.isBlank()).isTrue();
        }
    }

    @Test
    void stringFormatted_shouldWorkOnJava26() {
        String result = "Hello %s, version %d".formatted("Java", 26);
        assertThat(result).isEqualTo("Hello Java, version 26");
    }

    @Test
    void stringStripIndent_shouldWorkOnJava26() {
        // stripIndent removes the largest common whitespace prefix.
        // Without a trailing empty line (no \n at end), common indent = 4.
        String text = "    hello\n      world";
        String stripped = text.stripIndent();

        assertThat(stripped)
                .as("stripIndent should remove common leading whitespace (4 spaces)")
                .startsWith("hello")
                .contains("  world");
    }
}
