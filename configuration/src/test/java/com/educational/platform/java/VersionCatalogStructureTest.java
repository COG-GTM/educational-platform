package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structure and content of gradle/libs.versions.toml.
 * <p>
 * The version catalog is the single source of truth for dependency versions.
 * These tests ensure it remains well-formed, contains all required entries,
 * and does not regress to incompatible versions after the Java 26 upgrade.
 */
public class VersionCatalogStructureTest {

    @Test
    void versionCatalog_shouldHave_versionsSection() throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog must have a [versions] section")
                .contains("[versions]");
    }

    @Test
    void versionCatalog_shouldHave_pluginsSection() throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog must have a [plugins] section")
                .contains("[plugins]");
    }

    @Test
    void versionCatalog_shouldNotHave_emptyVersionValues() throws IOException {
        List<String> lines = Files.readAllLines(findVersionCatalog());

        Pattern emptyVersion = Pattern.compile("^\\s*\\w+\\s*=\\s*\"\"\\s*$");
        for (String line : lines) {
            assertThat(emptyVersion.matcher(line).matches())
                    .as("Version catalog should not have empty version values, but found: %s", line)
                    .isFalse();
        }
    }

    @Test
    void versionCatalog_shouldNotContain_snapshotVersions() throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog should not contain SNAPSHOT versions in production")
                .doesNotContainIgnoringCase("SNAPSHOT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"archunit", "mockito", "assertj", "spring"})
    void versionCatalog_shouldDefine_requiredDependency(String dependency) throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog should define version for: %s", dependency)
                .contains(dependency);
    }

    @Test
    void versionCatalog_archunit_shouldNotBe_belowMinimumForJava26() throws IOException {
        String content = readVersionCatalog();

        // ArchUnit < 1.4.2 cannot parse class file major version 70 (Java 26)
        assertThat(content)
                .as("ArchUnit version must not be below 1.4.2 (Java 26 minimum)")
                .doesNotContainPattern("archunit\\s*=\\s*\"1\\.4\\.[01]\"")
                .doesNotContainPattern("archunit\\s*=\\s*\"1\\.[0-3]\\.");
    }

    @Test
    void versionCatalog_versions_shouldFollow_semverFormat() throws IOException {
        List<String> lines = Files.readAllLines(findVersionCatalog());
        Pattern versionLine = Pattern.compile("^\\s*\\w+\\s*=\\s*\"([^\"]+)\"\\s*$");

        for (String line : lines) {
            var matcher = versionLine.matcher(line);
            if (matcher.matches()) {
                String version = matcher.group(1);
                // Must be a valid semver-ish format (X.Y.Z or X.Y)
                assertThat(version)
                        .as("Version '%s' should follow semver format", version)
                        .matches("\\d+\\.\\d+(\\.\\d+)?([.-]\\w+)?");
            }
        }
    }

    @Test
    void versionCatalog_shouldNotContain_duplicateKeys() throws IOException {
        List<String> lines = Files.readAllLines(findVersionCatalog());
        Pattern keyPattern = Pattern.compile("^\\s*(\\w+)\\s*=");

        var keys = lines.stream()
                .map(keyPattern::matcher)
                .filter(java.util.regex.Matcher::find)
                .map(m -> m.group(1))
                .toList();

        assertThat(keys)
                .as("Version catalog should not have duplicate keys")
                .doesNotHaveDuplicates();
    }

    @Test
    void versionCatalog_springDependencyManagement_shouldBeDefined() throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog should define spring-dependency-management plugin")
                .contains("springDependencyManagementPlugin")
                .contains("io.spring.dependency-management");
    }

    @Test
    void versionCatalog_shouldNotReference_oldArchunitVersion141() throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog must not reference old ArchUnit 1.4.1")
                .doesNotContain("\"1.4.1\"");
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findVersionCatalog());
    }

    private Path findVersionCatalog() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("gradle/libs.versions.toml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Paths.get("gradle/libs.versions.toml");
    }
}
