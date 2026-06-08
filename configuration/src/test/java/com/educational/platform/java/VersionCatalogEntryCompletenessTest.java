package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that every expected entry in gradle/libs.versions.toml is present
 * and that no entries were accidentally removed during the Java 26 upgrade.
 * <p>
 * The upgrade changed only the {@code archunit} version from 1.4.1 to 1.4.2.
 * This test ensures all other version entries remain intact.
 */
public class VersionCatalogEntryCompletenessTest {

    @ParameterizedTest(name = "Version catalog should contain entry: {0}")
    @ValueSource(strings = {
            "spring",
            "restAssured",
            "mockito",
            "assertj",
            "archunit",
            "jsonwebtoken",
            "jaxbApi",
            "passay",
            "springDependencyManagementPlugin",
            "springDoc"
    })
    void versionCatalog_shouldContain_entry(String key) throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog should contain entry for '%s'", key)
                .contains(key);
    }

    @ParameterizedTest(name = "Version catalog entry {0} should have value \"{1}\"")
    @CsvSource({
            "spring, 4.0.1",
            "restAssured, 6.0.0",
            "mockito, 5.19.0",
            "assertj, 3.27.3",
            "archunit, 1.4.2",
            "passay, 1.6.4",
            "springDependencyManagementPlugin, 1.1.7",
            "springDoc, 3.0.2"
    })
    void versionCatalog_entry_shouldHaveExpectedValue(String key, String expectedVersion) throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Version catalog entry '%s' should have value \"%s\"", key, expectedVersion)
                .containsPattern(key + "\\s*=\\s*\"" + Pattern.quote(expectedVersion) + "\"");
    }

    @Test
    void versionCatalog_shouldHave_exactNumberOfVersionEntries() throws IOException {
        List<String> lines = Files.readAllLines(findVersionCatalog());
        Pattern versionEntry = Pattern.compile("^\\s*\\w+\\s*=\\s*\"[^\"]+\"\\s*$");

        long versionCount = lines.stream()
                .filter(line -> versionEntry.matcher(line).matches())
                .count();

        assertThat(versionCount)
                .as("Version catalog should have exactly 10 version entries (no accidental removals)")
                .isEqualTo(10);
    }

    @Test
    void versionCatalog_plugins_shouldDefine_springDependencyManagement() throws IOException {
        String content = readVersionCatalog();

        assertThat(content)
                .as("Plugins section should define springdependencies")
                .containsPattern("springdependencies\\s*=");

        assertThat(content)
                .as("Plugin should reference io.spring.dependency-management")
                .contains("io.spring.dependency-management");
    }

    @Test
    void versionCatalog_archunitEntry_shouldNotBeOldVersion() throws IOException {
        String content = readVersionCatalog();
        Pattern archunitPattern = Pattern.compile("archunit\\s*=\\s*\"(\\d+\\.\\d+\\.\\d+)\"");
        Matcher matcher = archunitPattern.matcher(content);

        assertThat(matcher.find())
                .as("Should find archunit version entry")
                .isTrue();

        String version = matcher.group(1);
        assertThat(version)
                .as("ArchUnit version should not be the pre-upgrade 1.4.1")
                .isNotEqualTo("1.4.1");

        assertThat(version)
                .as("ArchUnit version should be 1.4.2 for Java 26 support")
                .isEqualTo("1.4.2");
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
