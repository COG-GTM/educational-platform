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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive regression guards that prevent partial Java 26 upgrades.
 * <p>
 * A partial upgrade — where some files reference the new version but others
 * still reference the old one — is the most common failure mode for version
 * bumps. These tests cross-reference multiple configuration files to ensure
 * all version references are coherent and no stale pre-upgrade values remain.
 */
public class VersionUpgradeRegressionGuardTest {

    private static final int CURRENT_JAVA_VERSION = 26;
    private static final int PREVIOUS_JAVA_VERSION = 25;
    private static final String CURRENT_GRADLE_VERSION = "9.5.1";
    private static final String PREVIOUS_GRADLE_VERSION = "9.2.1";
    private static final String CURRENT_ARCHUNIT_VERSION = "1.4.2";
    private static final String PREVIOUS_ARCHUNIT_VERSION = "1.4.1";

    // --- No stale Java version references ---

    @Test
    void buildGradle_shouldNotContain_previousJavaVersionConstant() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));

        assertThat(content)
                .as("build.gradle.kts should not reference VERSION_%d", PREVIOUS_JAVA_VERSION)
                .doesNotContain("VERSION_" + PREVIOUS_JAVA_VERSION);
    }

    @Test
    void readme_shouldNotContain_previousJavaVersion() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("README.md"));

        assertThat(content)
                .as("README should not reference Java %d", PREVIOUS_JAVA_VERSION)
                .doesNotContain("Java " + PREVIOUS_JAVA_VERSION);
    }

    @Test
    void readme_shouldNotContain_java21InInstallSection() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("README.md"));

        assertThat(content)
                .as("README should not reference Java 21 in install instructions")
                .doesNotContainPattern("(?i)install\\s+java\\s+21");
    }

    // --- No stale Gradle version references ---

    @Test
    void wrapperProperties_shouldNotContain_previousGradleVersion() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        String content = Files.readString(wrapperProps);

        assertThat(content)
                .as("gradle-wrapper.properties should not reference old Gradle %s", PREVIOUS_GRADLE_VERSION)
                .doesNotContain("gradle-" + PREVIOUS_GRADLE_VERSION);
    }

    // --- No stale ArchUnit version ---

    @Test
    void versionCatalog_shouldNotContain_previousArchunitVersion() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));

        assertThat(content)
                .as("libs.versions.toml should not reference old ArchUnit %s", PREVIOUS_ARCHUNIT_VERSION)
                .doesNotContain("\"" + PREVIOUS_ARCHUNIT_VERSION + "\"");
    }

    // --- Cross-file coherence ---

    @Test
    void sourceAndTargetCompatibility_shouldBeIdentical() throws IOException {
        String buildContent = Files.readString(findProjectRoot().resolve("build.gradle.kts"));

        Pattern sourcePattern = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.(VERSION_\\d+)");
        Pattern targetPattern = Pattern.compile("targetCompatibility\\s*=\\s*JavaVersion\\.(VERSION_\\d+)");

        Matcher sourceMatcher = sourcePattern.matcher(buildContent);
        Matcher targetMatcher = targetPattern.matcher(buildContent);

        assertThat(sourceMatcher.find()).isTrue();
        assertThat(targetMatcher.find()).isTrue();

        assertThat(sourceMatcher.group(1))
                .as("sourceCompatibility must equal targetCompatibility")
                .isEqualTo(targetMatcher.group(1));
    }

    @Test
    void buildJavaVersion_shouldMatch_runtimeJavaVersion() {
        int runtimeFeature = Runtime.version().feature();

        assertThat(runtimeFeature)
                .as("Runtime Java version should match the project target Java %d", CURRENT_JAVA_VERSION)
                .isEqualTo(CURRENT_JAVA_VERSION);
    }

    @Test
    void gradleWrapper_shouldBeCompatibleWith_buildJavaTarget() throws IOException {
        // Parse Gradle version from wrapper
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));
        String distUrl = props.getProperty("distributionUrl");

        Matcher matcher = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)").matcher(distUrl);
        assertThat(matcher.find()).isTrue();
        int gradleMajor = Integer.parseInt(matcher.group(1));
        int gradleMinor = Integer.parseInt(matcher.group(2));

        // Parse Java target from build
        String buildContent = Files.readString(findProjectRoot().resolve("build.gradle.kts"));
        Matcher javaMatcher = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(javaMatcher.find()).isTrue();
        int javaTarget = Integer.parseInt(javaMatcher.group(1));

        // Enforce compatibility: Java 26 requires Gradle >= 9.4
        if (javaTarget >= 26) {
            assertThat(gradleMajor)
                    .as("Gradle major version must be >= 9 for Java %d", javaTarget)
                    .isGreaterThanOrEqualTo(9);
            if (gradleMajor == 9) {
                assertThat(gradleMinor)
                        .as("Gradle 9.x minor must be >= 4 for Java %d", javaTarget)
                        .isGreaterThanOrEqualTo(4);
            }
        }
    }

    @Test
    void archunitVersion_shouldSupportCurrentJavaBytecode() throws IOException {
        String catalog = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));

        Matcher matcher = Pattern.compile("archunit\\s*=\\s*\"(\\d+)\\.(\\d+)\\.(\\d+)\"").matcher(catalog);
        assertThat(matcher.find()).as("ArchUnit version should be parseable from catalog").isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        // ArchUnit 1.4.2 is the minimum for class file major version 70 (Java 26)
        boolean supportsJava26 = (major > 1)
                || (major == 1 && minor > 4)
                || (major == 1 && minor == 4 && patch >= 2);

        assertThat(supportsJava26)
                .as("ArchUnit %d.%d.%d must support Java 26 bytecode (>= 1.4.2)", major, minor, patch)
                .isTrue();
    }

    // --- Boundary condition: version numbers parsed correctly ---

    @ParameterizedTest(name = "Version string \"{0}\" should parse as valid semver")
    @CsvSource({
            "'1.4.2', 1, 4, 2",
            "'9.5.1', 9, 5, 1",
            "'4.0.1', 4, 0, 1",
            "'5.19.0', 5, 19, 0",
            "'3.27.3', 3, 27, 3"
    })
    void versionString_shouldParseCorrectly(String version, int expectedMajor,
                                            int expectedMinor, int expectedPatch) {
        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)").matcher(version);
        assertThat(matcher.find()).isTrue();

        assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedMajor);
        assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(expectedMinor);
        assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(expectedPatch);
    }

    @ParameterizedTest(name = "Invalid version \"{0}\" should not match semver pattern")
    @ValueSource(strings = {"", "abc", "1.2", "1.2.3.4.5", "x.y.z"})
    void invalidVersion_shouldNotParse(String input) {
        Matcher matcher = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$").matcher(input);
        assertThat(matcher.find())
                .as("'%s' should not parse as a valid semver triple", input)
                .isFalse();
    }

    // --- Module-level consistency ---

    @Test
    void allSubmoduleBuildFiles_shouldNotOverride_javaVersion() throws IOException {
        Path root = findProjectRoot();

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .filter(p -> !p.equals(root.resolve("build.gradle.kts")))
                    .toList();

            for (Path buildFile : buildFiles) {
                String content = Files.readString(buildFile);
                // Submodule build files should NOT independently set sourceCompatibility
                // because it's set at the root level for all subprojects
                if (content.contains("sourceCompatibility")) {
                    // If a submodule does set it, it must also be VERSION_26
                    assertThat(content)
                            .as("Submodule %s overrides sourceCompatibility; must be VERSION_%d",
                                    buildFile, CURRENT_JAVA_VERSION)
                            .contains("VERSION_" + CURRENT_JAVA_VERSION)
                            .doesNotContain("VERSION_" + PREVIOUS_JAVA_VERSION);
                }
            }
        }
    }

    @Test
    void versionCatalog_shouldNotContain_anyOldKnownVersions() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));

        // Guard against stale version values that were part of the pre-upgrade state
        assertThat(content)
                .doesNotContain("\"" + PREVIOUS_ARCHUNIT_VERSION + "\"");
    }

    @Test
    void gradleWrapper_distributionUrl_shouldUseHttps() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));

        String distributionUrl = props.getProperty("distributionUrl");

        assertThat(distributionUrl)
                .as("Distribution URL must use HTTPS for secure downloads")
                .startsWith("https");
    }

    @Test
    void gradleWrapper_currentVersion_shouldNotBeLessThan_minimum() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));

        String distUrl = props.getProperty("distributionUrl");
        Matcher matcher = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)").matcher(distUrl);
        assertThat(matcher.find()).isTrue();

        String version = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);

        assertThat(version)
                .as("Gradle version should be the expected %s", CURRENT_GRADLE_VERSION)
                .isEqualTo(CURRENT_GRADLE_VERSION);
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
