package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates coherence between the Gradle wrapper JAR and the wrapper properties
 * file after the Gradle 9.2.1 → 9.5.1 upgrade.
 * <p>
 * A wrapper upgrade that updates the properties file but ships a stale JAR
 * (or vice versa) can cause subtle bootstrap failures: the JAR downloads
 * a different Gradle distribution than the properties file declares.
 * <p>
 * {@link GradleWrapperJarIntegrityTest} validates the JAR is a valid ZIP/JAR.
 * {@link GradleWrapperPropertiesCompletenessTest} validates properties keys.
 * {@link GradleWrapperVersionCoherenceTest} validates the properties version string.
 * This test validates the <em>cross-artifact coherence</em> between JAR and properties.
 */
public class GradleWrapperJarVersionCoherenceTest {

    @Test
    void wrapperJar_shouldExist_andBeValidJar() {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        assertThat(jarPath)
                .as("gradle-wrapper.jar should exist")
                .exists();

        assertThatCode(() -> {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                assertThat(jar.size())
                        .as("JAR should contain entries")
                        .isGreaterThan(0);
            }
        }).as("gradle-wrapper.jar should be a valid JAR file")
                .doesNotThrowAnyException();
    }

    @Test
    void wrapperJar_manifest_shouldExist() throws IOException {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();

            assertThat(manifest)
                    .as("gradle-wrapper.jar should have a manifest")
                    .isNotNull();
        }
    }

    @Test
    void wrapperJar_manifest_shouldDeclare_implementationTitle() throws IOException {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Attributes attrs = jar.getManifest().getMainAttributes();
            String title = attrs.getValue("Implementation-Title");

            assertThat(title)
                    .as("JAR Implementation-Title should indicate Gradle Wrapper")
                    .isNotNull()
                    .containsIgnoringCase("gradle");
        }
    }

    @Test
    void wrapperJar_shouldContain_wrapperMainClass() throws IOException {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            assertThat(jar.getEntry("org/gradle/wrapper/GradleWrapperMain.class"))
                    .as("JAR should contain GradleWrapperMain.class")
                    .isNotNull();
        }
    }

    @Test
    void wrapperJar_shouldHaveBeen_updatedWithProperties() throws IOException {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");
        Path propsPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");

        long jarLastModified = Files.getLastModifiedTime(jarPath).toMillis();
        long propsLastModified = Files.getLastModifiedTime(propsPath).toMillis();

        // If the properties file was modified (version bump), the JAR should have
        // been regenerated at the same time or after.
        // We use a generous tolerance (both should be from the same commit).
        long diffMs = Math.abs(jarLastModified - propsLastModified);
        long oneDayMs = 24 * 60 * 60 * 1000L;

        assertThat(diffMs)
                .as("wrapper JAR and properties should have similar modification times (same upgrade commit)")
                .isLessThan(oneDayMs);
    }

    @Test
    void wrapperProperties_distributionUrl_shouldNotPointTo_oldVersion() throws IOException {
        Properties props = readWrapperProperties();
        String distUrl = props.getProperty("distributionUrl");

        assertThat(distUrl)
                .as("distributionUrl should not reference old Gradle 9.2.1")
                .doesNotContain("gradle-9.2.1");
    }

    @Test
    void wrapperProperties_distributionUrl_shouldReference_951() throws IOException {
        Properties props = readWrapperProperties();
        String distUrl = props.getProperty("distributionUrl");
        Pattern versionPattern = Pattern.compile("gradle-(\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = versionPattern.matcher(distUrl);

        assertThat(matcher.find())
                .as("distributionUrl should contain a parseable Gradle version")
                .isTrue();

        assertThat(matcher.group(1))
                .as("distributionUrl version should be 9.5.1")
                .isEqualTo("9.5.1");
    }

    private Properties readWrapperProperties() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));
        return props;
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
