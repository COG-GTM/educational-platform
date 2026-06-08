package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-file consistency tests ensuring all version references across
 * the project are aligned after the Java 26 upgrade.
 * <p>
 * Checks that README, build.gradle.kts, gradle-wrapper.properties, and
 * libs.versions.toml all reference compatible and consistent versions.
 * Prevents partial upgrades where one file is updated but others are not.
 */
public class VersionConsistencyTest {

    private static final String EXPECTED_JAVA_VERSION = "26";
    private static final String EXPECTED_GRADLE_VERSION = "9.5.1";
    private static final String EXPECTED_ARCHUNIT_VERSION = "1.4.2";

    @Test
    void allFiles_shouldReference_sameJavaVersion() throws IOException {
        Path root = findProjectRoot();

        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        String readme = Files.readString(root.resolve("README.md"));

        // build.gradle.kts should reference VERSION_26
        assertThat(buildGradle)
                .as("build.gradle.kts should target Java %s", EXPECTED_JAVA_VERSION)
                .contains("VERSION_" + EXPECTED_JAVA_VERSION);

        // README should reference Java 26
        assertThat(readme)
                .as("README should reference Java %s", EXPECTED_JAVA_VERSION)
                .contains("Java " + EXPECTED_JAVA_VERSION);
    }

    @Test
    void readme_and_buildGradle_shouldNotReference_previousJavaVersion() throws IOException {
        Path root = findProjectRoot();

        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        String readme = Files.readString(root.resolve("README.md"));

        assertThat(buildGradle)
                .as("build.gradle.kts should not reference Java 25")
                .doesNotContain("VERSION_25");

        assertThat(readme)
                .as("README should not reference Java 25")
                .doesNotContain("Java 25");
    }

    @Test
    void gradleWrapperVersion_shouldBeCompatibleWith_java26() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));

        String distributionUrl = props.getProperty("distributionUrl");
        Pattern versionPattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = versionPattern.matcher(distributionUrl);

        assertThat(matcher.find())
                .as("Should be able to parse Gradle version from distributionUrl")
                .isTrue();

        String gradleVersion = matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        assertThat(gradleVersion)
                .as("Gradle version should be %s", EXPECTED_GRADLE_VERSION)
                .isEqualTo(EXPECTED_GRADLE_VERSION);
    }

    @Test
    void versionCatalog_archunitVersion_shouldMatch_expected() throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);

        assertThat(content)
                .as("Version catalog should declare archunit = \"%s\"", EXPECTED_ARCHUNIT_VERSION)
                .containsPattern("archunit\\s*=\\s*\"" + Pattern.quote(EXPECTED_ARCHUNIT_VERSION) + "\"");
    }

    @Test
    void buildGradle_sourceAndTarget_shouldMatch() throws IOException {
        Path buildGradle = findProjectRoot().resolve("build.gradle.kts");
        String content = Files.readString(buildGradle);

        Pattern sourcePattern = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.(VERSION_\\d+)");
        Pattern targetPattern = Pattern.compile("targetCompatibility\\s*=\\s*JavaVersion\\.(VERSION_\\d+)");

        Matcher sourceMatcher = sourcePattern.matcher(content);
        Matcher targetMatcher = targetPattern.matcher(content);

        assertThat(sourceMatcher.find()).as("sourceCompatibility should be declared").isTrue();
        assertThat(targetMatcher.find()).as("targetCompatibility should be declared").isTrue();

        String sourceVersion = sourceMatcher.group(1);
        String targetVersion = targetMatcher.group(1);

        assertThat(sourceVersion)
                .as("sourceCompatibility and targetCompatibility should be the same")
                .isEqualTo(targetVersion);
    }

    @Test
    void runtimeJavaVersion_shouldMatch_buildTargetVersion() throws IOException {
        Path buildGradle = findProjectRoot().resolve("build.gradle.kts");
        String content = Files.readString(buildGradle);

        Pattern pattern = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();

        int buildTargetVersion = Integer.parseInt(matcher.group(1));
        int runtimeVersion = Runtime.version().feature();

        assertThat(runtimeVersion)
                .as("Runtime Java version should be >= build target version (%d)", buildTargetVersion)
                .isGreaterThanOrEqualTo(buildTargetVersion);
    }

    @Test
    void readme_installSection_shouldMatch_buildGradleVersion() throws IOException {
        Path root = findProjectRoot();
        String readme = Files.readString(root.resolve("README.md"));
        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));

        // Extract version number from build.gradle.kts
        Pattern pattern = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Matcher matcher = pattern.matcher(buildGradle);
        assertThat(matcher.find()).isTrue();
        String buildVersion = matcher.group(1);

        // README install section should reference the same version
        assertThat(readme)
                .as("README install instructions should reference Java %s", buildVersion)
                .containsPattern("(?i)install\\s+java\\s+" + buildVersion);
    }

    @Test
    void configurationBuildGradle_shouldDeclare_archunitFromVersionCatalog() throws IOException {
        Path configBuild = findProjectRoot().resolve("configuration/build.gradle.kts");
        String content = Files.readString(configBuild);

        assertThat(content)
                .as("configuration build.gradle.kts should use archunit from version catalog")
                .contains("archunit-junit5");

        assertThat(content)
                .as("configuration build.gradle.kts should reference libs.versions.archunit")
                .contains("libs.versions.archunit");
    }

    @Test
    void allOldVersionReferences_shouldBeAbsent() throws IOException {
        Path root = findProjectRoot();

        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        String wrapperProps = Files.readString(root.resolve("gradle/wrapper/gradle-wrapper.properties"));
        String catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));

        assertThat(buildGradle)
                .as("build.gradle.kts should not reference old Java 25")
                .doesNotContain("VERSION_25");

        assertThat(wrapperProps)
                .as("wrapper properties should not reference old Gradle 9.2.1")
                .doesNotContain("gradle-9.2.1");

        assertThat(catalog)
                .as("version catalog should not contain archunit 1.4.1")
                .doesNotContainPattern("archunit\\s*=\\s*\"1\\.4\\.1\"");
    }

    @Test
    void gradleVersion_shouldBeAtLeast_minimumForJava26_acrossAllReferences() throws IOException {
        // Java 26 requires Gradle >= 9.4.0
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));

        String distributionUrl = props.getProperty("distributionUrl");
        Pattern versionPattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = versionPattern.matcher(distributionUrl);
        assertThat(matcher.find()).isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));

        // Gradle 9.4.0 is minimum for Java 26
        boolean isCompatible = (major > 9) || (major == 9 && minor >= 4);
        assertThat(isCompatible)
                .as("Gradle %d.%d should be >= 9.4 for Java 26 support", major, minor)
                .isTrue();
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
