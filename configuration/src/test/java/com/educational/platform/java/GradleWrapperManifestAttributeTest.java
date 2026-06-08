package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Gradle wrapper JAR manifest attributes and internal structure
 * for the 9.5.1 wrapper shipped with the Java 26 upgrade.
 * <p>
 * Gradle 9.5+ changed the wrapper invocation from classpath-based
 * ({@code -classpath ... org.gradle.wrapper.GradleWrapperMain}) to direct JAR
 * invocation ({@code -jar gradle/wrapper/gradle-wrapper.jar}). This requires
 * the JAR's manifest to declare a valid {@code Main-Class} attribute.
 * <p>
 * This test prevents shipping a wrapper JAR that is structurally valid but
 * functionally broken due to missing or incorrect manifest entries.
 */
public class GradleWrapperManifestAttributeTest {

    @Test
    void manifest_shouldDeclare_mainClassAttribute() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            assertThat(manifest).as("Manifest should be present").isNotNull();

            String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);

            assertThat(mainClass)
                    .as("Wrapper JAR must declare Main-Class for -jar invocation")
                    .isNotNull()
                    .isNotBlank();
        }
    }

    @Test
    void manifest_mainClass_shouldBeGradleWrapperMain() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String mainClass = jar.getManifest().getMainAttributes()
                    .getValue(Attributes.Name.MAIN_CLASS);

            assertThat(mainClass)
                    .as("Main-Class should be the Gradle wrapper entry point")
                    .isEqualTo("org.gradle.wrapper.GradleWrapperMain");
        }
    }

    @Test
    void manifest_shouldDeclare_implementationTitle() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String title = jar.getManifest().getMainAttributes()
                    .getValue(Attributes.Name.IMPLEMENTATION_TITLE);

            assertThat(title)
                    .as("Implementation-Title should identify this as the Gradle wrapper")
                    .isNotNull()
                    .containsIgnoringCase("gradle");
        }
    }

    @Test
    void manifest_shouldDeclare_manifestVersion() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String manifestVersion = jar.getManifest().getMainAttributes()
                    .getValue(Attributes.Name.MANIFEST_VERSION);

            assertThat(manifestVersion)
                    .as("Manifest-Version should be 1.0")
                    .isEqualTo("1.0");
        }
    }

    @Test
    void wrapperJar_shouldContain_gradleWrapperMainClass() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry mainClassEntry = jar.getJarEntry(
                    "org/gradle/wrapper/GradleWrapperMain.class");

            assertThat(mainClassEntry)
                    .as("JAR should contain the GradleWrapperMain class file")
                    .isNotNull();
        }
    }

    @Test
    void wrapperJar_shouldContain_wrapperPackageClasses() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            long wrapperClasses = jar.stream()
                    .filter(entry -> entry.getName().startsWith("org/gradle/wrapper/"))
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .count();

            assertThat(wrapperClasses)
                    .as("JAR should contain multiple wrapper classes")
                    .isGreaterThan(5);
        }
    }

    @Test
    void wrapperJar_shouldNotContain_testClasses() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            long testClasses = jar.stream()
                    .filter(entry -> entry.getName().contains("Test"))
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .count();

            assertThat(testClasses)
                    .as("Wrapper JAR should not contain test classes")
                    .isZero();
        }
    }

    @Test
    void wrapperJar_classFiles_shouldNotExceedJava26MajorVersion() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // Check the GradleWrapperMain class file major version
            JarEntry entry = jar.getJarEntry("org/gradle/wrapper/GradleWrapperMain.class");
            assertThat(entry).isNotNull();

            try (var is = jar.getInputStream(entry)) {
                var dis = new java.io.DataInputStream(is);
                int magic = dis.readInt();
                assertThat(magic).isEqualTo(0xCAFEBABE);

                dis.readUnsignedShort(); // minor
                int majorVersion = dis.readUnsignedShort();

                // Wrapper JAR classes should be compiled for a version <= Java 26
                // Gradle typically compiles the wrapper for an older target for broad compatibility
                assertThat(majorVersion)
                        .as("Wrapper class major version should be <= 70 (Java 26)")
                        .isLessThanOrEqualTo(70)
                        .isGreaterThanOrEqualTo(52); // At least Java 8
            }
        }
    }

    @Test
    void wrapperJar_shouldContain_downloadClass() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            // The wrapper needs a download capability to fetch the full Gradle distribution
            boolean hasDownloadRelatedClass = jar.stream()
                    .anyMatch(entry ->
                            entry.getName().contains("Download") ||
                                    entry.getName().contains("download") ||
                                    entry.getName().contains("Install") ||
                                    entry.getName().contains("install"));

            assertThat(hasDownloadRelatedClass)
                    .as("Wrapper JAR should contain distribution download/install classes")
                    .isTrue();
        }
    }

    private Path findWrapperJar() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("gradle/wrapper/gradle-wrapper.jar");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        Path fallback = Paths.get("gradle/wrapper/gradle-wrapper.jar");
        assertThat(fallback).as("gradle-wrapper.jar should exist").exists();
        return fallback;
    }
}
