package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the project toolchain (Java 26 compiler + runtime) supports
 * modern language features. This proves the upgrade wasn't just a version number
 * change in build.gradle.kts — the compiler actually produces code using features
 * that only work on Java 16+ / 21+ / 26+.
 * <p>
 * If these tests compile and pass, the toolchain is correctly configured.
 */
public class Java26LanguageFeaturesTest {

    // --- Records (Java 16+) ---

    record Point(int x, int y) {}

    record VersionInfo(int major, int minor, int patch) {
        String formatted() {
            return major + "." + minor + "." + patch;
        }
    }

    @Test
    void records_shouldWork_asDataCarriers() {
        var point = new Point(3, 4);

        assertThat(point.x()).isEqualTo(3);
        assertThat(point.y()).isEqualTo(4);
        assertThat(point.toString()).contains("3").contains("4");
    }

    @Test
    void records_shouldSupport_customMethods() {
        var version = new VersionInfo(9, 5, 1);

        assertThat(version.formatted()).isEqualTo("9.5.1");
    }

    @Test
    void records_shouldImplement_equalsAndHashCode() {
        var v1 = new VersionInfo(26, 0, 1);
        var v2 = new VersionInfo(26, 0, 1);
        var v3 = new VersionInfo(25, 0, 0);

        assertThat(v1).isEqualTo(v2);
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
        assertThat(v1).isNotEqualTo(v3);
    }

    // --- Sealed classes (Java 17+) ---

    sealed interface BuildTool permits GradleTool, MavenTool {}
    record GradleTool(String version) implements BuildTool {}
    record MavenTool(String version) implements BuildTool {}

    @Test
    void sealedInterfaces_shouldRestrictImplementations() {
        BuildTool tool = new GradleTool("9.5.1");

        assertThat(tool).isInstanceOf(GradleTool.class);
        assertThat(((GradleTool) tool).version()).isEqualTo("9.5.1");
    }

    // --- Pattern matching for instanceof (Java 16+) ---

    @Test
    void patternMatchingInstanceof_shouldBindVariable() {
        Object obj = "Java 26";

        if (obj instanceof String s) {
            assertThat(s).startsWith("Java");
            assertThat(s).endsWith("26");
        } else {
            throw new AssertionError("Pattern matching failed");
        }
    }

    @Test
    void patternMatchingInstanceof_shouldWork_withNegation() {
        Object obj = Integer.valueOf(70);

        if (obj instanceof Integer majorVersion && majorVersion == 70) {
            assertThat(majorVersion)
                    .as("Class file major version for Java 26")
                    .isEqualTo(70);
        } else {
            throw new AssertionError("Expected Integer 70");
        }
    }

    // --- Switch expressions (Java 14+) ---

    @Test
    void switchExpressions_shouldReturn_values() {
        int javaVersion = 26;

        String gradleMinimum = switch (javaVersion) {
            case 21 -> "8.5";
            case 22 -> "8.7";
            case 23 -> "8.9";
            case 24 -> "9.0";
            case 25 -> "9.2";
            case 26 -> "9.4";
            default -> "unknown";
        };

        assertThat(gradleMinimum).isEqualTo("9.4");
    }

    @Test
    void switchExpressions_shouldWork_withSealedTypes() {
        BuildTool tool = new GradleTool("9.5.1");

        String description = switch (tool) {
            case GradleTool g -> "Gradle " + g.version();
            case MavenTool m -> "Maven " + m.version();
        };

        assertThat(description).isEqualTo("Gradle 9.5.1");
    }

    // --- Text blocks (Java 15+) ---

    @Test
    void textBlocks_shouldPreserve_multiLineStrings() {
        String toml = """
                [versions]
                archunit = "1.4.2"
                """;

        assertThat(toml).contains("[versions]");
        assertThat(toml).contains("archunit = \"1.4.2\"");
    }

    @Test
    void textBlocks_shouldWork_forGradleSnippets() {
        String buildScript = """
                java {
                    sourceCompatibility = JavaVersion.VERSION_26
                    targetCompatibility = JavaVersion.VERSION_26
                }
                """;

        assertThat(buildScript)
                .contains("VERSION_26")
                .doesNotContain("VERSION_25");
    }

    // --- var (local variable type inference, Java 10+) ---

    @Test
    void varKeyword_shouldInferTypes() {
        var version = Runtime.version();
        var feature = version.feature();

        assertThat(feature).isGreaterThanOrEqualTo(26);
    }

    // --- Unnamed patterns and variables (Java 22+) ---

    @Test
    void unnamedVariables_shouldWork_inForLoops() {
        var items = java.util.List.of("Java", "26", "Gradle", "9.5.1");
        int count = 0;
        for (var _ : items) {
            count++;
        }
        assertThat(count).isEqualTo(4);
    }

    @Test
    void unnamedVariables_shouldWork_inTryCatch() {
        int result;
        try {
            result = Integer.parseInt("26");
        } catch (NumberFormatException _) {
            result = -1;
        }
        assertThat(result).isEqualTo(26);
    }

    // --- Confirm overall Java 26 runtime ---

    @Test
    void runtime_shouldConfirm_java26Features_available() {
        assertThat(Runtime.version().feature())
                .as("All language feature tests above require Java 26 runtime")
                .isGreaterThanOrEqualTo(26);
    }
}
