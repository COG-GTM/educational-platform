package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper JAR can bootstrap on older JVMs.
 * <p>
 * The wrapper JAR must be compiled at a class file version lower than the project's
 * target (Java 26 / major version 70) because users need to run the wrapper to
 * download and install the correct Gradle version BEFORE the project JDK is
 * configured. Typically, the wrapper JAR targets Java 8 (major version 52).
 * <p>
 * After the Gradle 9.2.1 → 9.5.1 upgrade, this test ensures the new wrapper JAR
 * has not been accidentally compiled with Java 26, which would prevent bootstrapping
 * on machines with only Java 8–25 installed.
 */
public class GradleWrapperJarBootstrapCompatibilityTest {

    private static final int JAVA_8_CLASS_MAJOR_VERSION = 52;
    private static final int JAVA_26_CLASS_MAJOR_VERSION = 70;

    @Test
    void wrapperJar_shouldExist_andBeNonEmpty() throws IOException {
        Path jarPath = findWrapperJar();

        assertThat(jarPath)
                .as("gradle-wrapper.jar should exist")
                .exists();

        assertThat(Files.size(jarPath))
                .as("gradle-wrapper.jar should not be empty")
                .isGreaterThan(0);
    }

    @Test
    void wrapperJar_shouldBeAValidZipArchive() throws IOException {
        Path jarPath = findWrapperJar();

        int entryCount = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                zis.closeEntry();
            }
        }

        assertThat(entryCount)
                .as("gradle-wrapper.jar should contain entries (valid ZIP)")
                .isGreaterThan(0);
    }

    @Test
    void wrapperJar_classFiles_shouldNotTarget_java26() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var classEntry = jar.stream()
                    .filter(e -> e.getName().endsWith(".class"))
                    .findFirst();

            assertThat(classEntry)
                    .as("Wrapper JAR should contain at least one .class file")
                    .isPresent();

            try (InputStream is = jar.getInputStream(classEntry.get())) {
                DataInputStream dis = new DataInputStream(is);
                int magic = dis.readInt();
                assertThat(magic)
                        .as("Class file magic number")
                        .isEqualTo(0xCAFEBABE);

                dis.readUnsignedShort(); // minor version
                int majorVersion = dis.readUnsignedShort();

                assertThat(majorVersion)
                        .as("Wrapper JAR classes should NOT be compiled with Java 26 (would prevent bootstrap)")
                        .isLessThan(JAVA_26_CLASS_MAJOR_VERSION);
            }
        }
    }

    @Test
    void wrapperJar_classFiles_shouldTarget_java8OrHigher() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var classEntry = jar.stream()
                    .filter(e -> e.getName().endsWith(".class"))
                    .findFirst()
                    .orElseThrow();

            try (InputStream is = jar.getInputStream(classEntry)) {
                DataInputStream dis = new DataInputStream(is);
                dis.readInt(); // magic
                dis.readUnsignedShort(); // minor
                int majorVersion = dis.readUnsignedShort();

                assertThat(majorVersion)
                        .as("Wrapper JAR classes should target at least Java 8 (major version 52)")
                        .isGreaterThanOrEqualTo(JAVA_8_CLASS_MAJOR_VERSION);
            }
        }
    }

    @Test
    void wrapperJar_shouldContain_gradleWrapperMainClass() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            boolean hasWrapperClass = jar.stream()
                    .anyMatch(e -> e.getName().contains("GradleWrapperMain"));

            assertThat(hasWrapperClass)
                    .as("Wrapper JAR should contain GradleWrapperMain class")
                    .isTrue();
        }
    }

    @Test
    void wrapperJar_shouldHave_manifest() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();

            assertThat(manifest)
                    .as("Wrapper JAR should have a MANIFEST.MF")
                    .isNotNull();
        }
    }

    @Test
    void wrapperJar_fileSize_shouldBe_reasonable() throws IOException {
        Path jarPath = findWrapperJar();
        long size = Files.size(jarPath);

        // The wrapper JAR is typically between 40KB and 200KB
        assertThat(size)
                .as("Wrapper JAR size should be reasonable (not corrupt or bloated)")
                .isBetween(30_000L, 500_000L);
    }

    private Path findWrapperJar() {
        return findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");
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
