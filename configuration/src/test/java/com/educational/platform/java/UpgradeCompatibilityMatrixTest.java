package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the full version compatibility matrix as a cohesive unit after
 * the Java 26 upgrade.
 * <p>
 * Individual tests validate pairs (Java ↔ Gradle, Java ↔ ArchUnit, etc.).
 * This test validates that <em>all</em> upgraded components form a mutually
 * compatible set — a regression in any single dependency can be masked if
 * tests only check pairwise compatibility. For example, a Gradle version
 * that supports Java 26 but ships an ASM version that conflicts with
 * ArchUnit 1.4.2's bundled ASM would only be caught by testing the full
 * combination together.
 * <p>
 * The matrix:
 * <ul>
 *   <li>Java 26 (bytecode major version 70)</li>
 *   <li>Gradle ≥ 9.4.0 (minimum for Java 26)</li>
 *   <li>ArchUnit ≥ 1.4.2 (ASM support for class file version 70)</li>
 *   <li>AssertJ ≥ 3.27.0 (Java 26 assertion support)</li>
 *   <li>Mockito ≥ 5.19.0 (byte-buddy proxy generation on v70 bytecode)</li>
 *   <li>JUnit Jupiter ≥ 5.10 (Java 26 runtime support)</li>
 * </ul>
 */
public class UpgradeCompatibilityMatrixTest {

    @Test
    void fullMatrix_allVersions_shouldMeetJava26Minimums() throws IOException {
        Path root = findProjectRoot();

        // --- Java version ---
        int runtimeJava = Runtime.version().feature();
        assertThat(runtimeJava)
                .as("Runtime Java feature version")
                .isGreaterThanOrEqualTo(26);

        // --- Gradle version from wrapper ---
        Properties wrapperProps = new Properties();
        wrapperProps.load(Files.newInputStream(root.resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = wrapperProps.getProperty("distributionUrl");
        int[] gradle = parseVersion(distUrl, "gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        boolean gradleOk = gradle[0] > 9 || (gradle[0] == 9 && gradle[1] >= 4);
        assertThat(gradleOk)
                .as("Gradle %d.%d.%d must be >= 9.4.0 for Java 26", gradle[0], gradle[1], gradle[2])
                .isTrue();

        // --- ArchUnit from version catalog ---
        String catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));
        int[] archunit = parseCatalogVersion(catalog, "archunit");
        boolean archunitOk = archunit[0] > 1
                || (archunit[0] == 1 && archunit[1] > 4)
                || (archunit[0] == 1 && archunit[1] == 4 && archunit[2] >= 2);
        assertThat(archunitOk)
                .as("ArchUnit %d.%d.%d must be >= 1.4.2 for class file v70",
                        archunit[0], archunit[1], archunit[2])
                .isTrue();

        // --- AssertJ from version catalog ---
        int[] assertj = parseCatalogVersion(catalog, "assertj");
        boolean assertjOk = assertj[0] > 3
                || (assertj[0] == 3 && assertj[1] >= 27);
        assertThat(assertjOk)
                .as("AssertJ %d.%d.%d must be >= 3.27.0 for Java 26",
                        assertj[0], assertj[1], assertj[2])
                .isTrue();

        // --- Mockito from version catalog ---
        int[] mockito = parseCatalogVersion(catalog, "mockito");
        boolean mockitoOk = mockito[0] > 5
                || (mockito[0] == 5 && mockito[1] >= 19);
        assertThat(mockitoOk)
                .as("Mockito %d.%d.%d must be >= 5.19.0 for Java 26 byte-buddy support",
                        mockito[0], mockito[1], mockito[2])
                .isTrue();

        // --- JUnit Jupiter from classpath ---
        String jupiterVersion = resolveArtifactVersion("org.junit.jupiter", "junit-jupiter-api");
        assertThat(jupiterVersion).as("JUnit Jupiter version should be resolvable").isNotNull();
        String[] jParts = jupiterVersion.split("\\.");
        int jMajor = Integer.parseInt(jParts[0]);
        int jMinor = Integer.parseInt(jParts[1]);
        boolean junitOk = jMajor > 5 || (jMajor == 5 && jMinor >= 10);
        assertThat(junitOk)
                .as("JUnit Jupiter %s must be >= 5.10 for Java 26", jupiterVersion)
                .isTrue();
    }

    @Test
    void buildTarget_and_runtimeVersion_and_bytecodeVersion_shouldBeCoherent() throws IOException {
        Path root = findProjectRoot();

        // Extract build target from build.gradle.kts
        String buildContent = Files.readString(root.resolve("build.gradle.kts"));
        Matcher m = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(m.find()).isTrue();
        int buildTarget = Integer.parseInt(m.group(1));

        // Runtime version
        int runtimeFeature = Runtime.version().feature();

        // Bytecode formula: major = 44 + java version
        int expectedMajor = 44 + buildTarget;
        String classVersion = System.getProperty("java.class.version");
        int actualMajor = (int) Double.parseDouble(classVersion);

        assertThat(buildTarget).isEqualTo(runtimeFeature);
        assertThat(actualMajor).isEqualTo(expectedMajor);
    }

    @Test
    void gradleVersion_and_archunitVersion_shouldBothSupport_java26Bytecode() throws IOException {
        Path root = findProjectRoot();

        // Both Gradle and ArchUnit use ASM internally. They must both support
        // the same class file major version (70 for Java 26).
        Properties wrapperProps = new Properties();
        wrapperProps.load(Files.newInputStream(root.resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = wrapperProps.getProperty("distributionUrl");
        int[] gradle = parseVersion(distUrl, "gradle-(\\d+)\\.(\\d+)\\.(\\d+)");

        String catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));
        int[] archunit = parseCatalogVersion(catalog, "archunit");

        // Gradle 9.4+ and ArchUnit 1.4.2+ both support class file major version 70
        assertThat(gradle[0] * 100 + gradle[1])
                .as("Gradle version (9.4+ needed)")
                .isGreaterThanOrEqualTo(904);

        assertThat(archunit[0] * 10000 + archunit[1] * 100 + archunit[2])
                .as("ArchUnit version (1.4.2+ needed)")
                .isGreaterThanOrEqualTo(10402);
    }

    @Test
    void springBomVersion_shouldBeJava26Compatible() throws IOException {
        String catalog = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
        int[] spring = parseCatalogVersion(catalog, "spring");

        // Spring Boot 4.x supports Java 26
        assertThat(spring[0])
                .as("Spring Boot major version should be >= 4 for Java 26")
                .isGreaterThanOrEqualTo(4);
    }

    private int[] parseVersion(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        assertThat(m.find()).isTrue();
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
        };
    }

    private int[] parseCatalogVersion(String catalog, String key) {
        Pattern p = Pattern.compile(key + "\\s*=\\s*\"(\\d+)\\.(\\d+)\\.(\\d+)\"");
        Matcher m = p.matcher(catalog);
        assertThat(m.find()).as("Version catalog should contain '%s'", key).isTrue();
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
        };
    }

    private String resolveArtifactVersion(String groupId, String artifactId) {
        String pomPath = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        // Fallback: Package.getImplementationVersion() of a known class
        try {
            String knownClass = switch (artifactId) {
                case "junit-jupiter-api" -> "org.junit.jupiter.api.Test";
                case "junit-jupiter-params" -> "org.junit.jupiter.params.ParameterizedTest";
                case "junit-jupiter-engine" -> "org.junit.jupiter.engine.JupiterTestEngine";
                case "junit-platform-engine" -> "org.junit.platform.engine.TestEngine";
                case "junit-platform-launcher" -> "org.junit.platform.launcher.Launcher";
                default -> null;
            };
            if (knownClass != null) {
                Class<?> clazz = Class.forName(knownClass);
                Package pkg = clazz.getPackage();
                if (pkg != null && pkg.getImplementationVersion() != null) {
                    return pkg.getImplementationVersion();
                }
            }
        } catch (ClassNotFoundException ignored) {
        }

        return null;
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
