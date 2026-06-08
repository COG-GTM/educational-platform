package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against incomplete Java 26 upgrades by validating that every
 * production file modified in the upgrade PR contains its expected new value.
 * <p>
 * {@link VersionConsistencyTest} and {@link VersionUpgradeAtomicCoherenceTest}
 * cross-check version agreement between files. This test approaches the same
 * problem from the <em>change manifest</em> perspective: for each production
 * file that the upgrade PR must modify, it asserts both the presence of the
 * new value and the absence of the old value in a single parameterized test.
 * <p>
 * If a future PR accidentally reverts one file while keeping the rest, this
 * test pinpoints exactly which file regressed.
 */
public class Java26UpgradeCompletionGuardTest {

    @Test
    void buildGradleKts_shouldContain_version26_andNot_version25() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));

        assertThat(content)
                .as("build.gradle.kts should contain VERSION_26")
                .contains("VERSION_26");

        assertThat(content)
                .as("build.gradle.kts should not contain VERSION_25")
                .doesNotContain("VERSION_25");
    }

    @Test
    void readme_shouldContain_java26_andNot_java25OrJava21() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("README.md"));

        assertThat(content)
                .as("README should reference Java 26")
                .contains("Java 26");

        assertThat(content)
                .as("README should not reference Java 25")
                .doesNotContain("Java 25");

        assertThat(content)
                .as("README install section should not reference Java 21")
                .doesNotContainPattern("(?i)install\\s+java\\s+21");
    }

    @Test
    void libsVersionsToml_shouldContain_archunit142_andNot_141() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));

        assertThat(content)
                .as("libs.versions.toml should declare archunit 1.4.2")
                .containsPattern("archunit\\s*=\\s*\"1\\.4\\.2\"");

        assertThat(content)
                .as("libs.versions.toml should not contain old archunit 1.4.1")
                .doesNotContain("\"1.4.1\"");
    }

    @Test
    void wrapperProperties_shouldContain_gradle951_andNot_921() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));

        String distUrl = props.getProperty("distributionUrl");

        assertThat(distUrl)
                .as("Wrapper distribution URL should reference Gradle 9.5.1")
                .contains("gradle-9.5.1");

        assertThat(distUrl)
                .as("Wrapper distribution URL should not reference old Gradle 9.2.1")
                .doesNotContain("gradle-9.2.1");
    }

    @Test
    void wrapperProperties_shouldContain_newRetryProperties() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));

        assertThat(props.getProperty("retries"))
                .as("Wrapper should have 'retries' property (new in 9.5.1)")
                .isNotNull();

        assertThat(props.getProperty("retryBackOffMs"))
                .as("Wrapper should have 'retryBackOffMs' property (new in 9.5.1)")
                .isNotNull();
    }

    @Test
    void configurationBuildGradle_shouldContain_allThreeNewDeps() throws IOException {
        String content = Files.readString(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        assertThat(content)
                .as("configuration/build.gradle.kts should include junit-jupiter-params")
                .contains("junit-jupiter-params");

        assertThat(content)
                .as("configuration/build.gradle.kts should include junit-jupiter-engine")
                .contains("junit-jupiter-engine");

        assertThat(content)
                .as("configuration/build.gradle.kts should include assertj-core")
                .contains("assertj-core");
    }

    @ParameterizedTest(name = "File ''{0}'' should exist at project root")
    @CsvSource({
            "build.gradle.kts",
            "README.md",
            "gradle/libs.versions.toml",
            "gradle/wrapper/gradle-wrapper.properties",
            "configuration/build.gradle.kts"
    })
    void upgradedFile_shouldExist(String relativePath) {
        Path file = findProjectRoot().resolve(relativePath);

        assertThat(file)
                .as("Upgraded file should exist: %s", relativePath)
                .exists();
    }

    @Test
    void allUpgradedValues_shouldBeCoherent_withRuntimeVersion() throws IOException {
        Path root = findProjectRoot();

        // Extract Java version from build script
        String buildContent = Files.readString(root.resolve("build.gradle.kts"));
        Matcher m = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(m.find()).isTrue();
        int buildJava = Integer.parseInt(m.group(1));

        // Verify runtime matches the build target
        int runtimeJava = Runtime.version().feature();
        assertThat(runtimeJava)
                .as("Runtime Java version should match build target")
                .isEqualTo(buildJava);

        // Verify bytecode formula
        int expectedClassFileMajor = 44 + buildJava;
        String classVersion = System.getProperty("java.class.version");
        int actualMajor = (int) Double.parseDouble(classVersion);
        assertThat(actualMajor)
                .as("Class file major version should equal 44 + %d", buildJava)
                .isEqualTo(expectedClassFileMajor);
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path settingsFile = current.resolve("settings.gradle.kts");
            Path gradleDir = current.resolve("gradle/wrapper");
            if (Files.exists(settingsFile) || Files.isDirectory(gradleDir)) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
