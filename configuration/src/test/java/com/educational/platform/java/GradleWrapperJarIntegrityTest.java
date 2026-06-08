package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the integrity of the Gradle wrapper JAR shipped in the repository.
 * <p>
 * When upgrading Gradle from 9.2.1 to 9.5.1, the wrapper JAR is replaced.
 * This test ensures the new JAR is a valid archive with the expected manifest
 * and entry point, preventing a corrupted or truncated JAR from breaking builds.
 */
public class GradleWrapperJarIntegrityTest {

    @Test
    void wrapperJar_shouldBeAValidJarArchive() {
        Path jarPath = findWrapperJar();

        assertThatCode(() -> {
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                assertThat(jar.size())
                        .as("Wrapper JAR should contain entries")
                        .isGreaterThan(0);
            }
        }).as("gradle-wrapper.jar should be openable as a valid JAR")
                .doesNotThrowAnyException();
    }

    @Test
    void wrapperJar_shouldContainManifest() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();

            assertThat(manifest)
                    .as("Wrapper JAR should contain a META-INF/MANIFEST.MF")
                    .isNotNull();
        }
    }

    @Test
    void wrapperJar_shouldHaveNonTrivialSize() {
        Path jarPath = findWrapperJar();

        long sizeBytes = jarPath.toFile().length();

        assertThat(sizeBytes)
                .as("Wrapper JAR should not be empty or trivially small (likely corrupted)")
                .isGreaterThan(10_000L);
    }

    @Test
    void wrapperJar_shouldNotBeExcessivelyLarge() {
        Path jarPath = findWrapperJar();

        long sizeBytes = jarPath.toFile().length();

        assertThat(sizeBytes)
                .as("Wrapper JAR should not be excessively large (< 500 KB for a wrapper)")
                .isLessThan(500_000L);
    }

    @Test
    void wrapperJar_shouldStartWithPKSignature() throws IOException {
        Path jarPath = findWrapperJar();
        byte[] header = Files.readAllBytes(jarPath);

        assertThat(header.length).isGreaterThan(4);
        assertThat(header[0]).as("First byte of ZIP/JAR (P)").isEqualTo((byte) 'P');
        assertThat(header[1]).as("Second byte of ZIP/JAR (K)").isEqualTo((byte) 'K');
    }

    @Test
    void wrapperJar_shouldResideNextToProperties() {
        Path jarPath = findWrapperJar();
        Path propsPath = jarPath.getParent().resolve("gradle-wrapper.properties");

        assertThat(propsPath)
                .as("gradle-wrapper.properties should exist alongside the JAR")
                .exists();
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
