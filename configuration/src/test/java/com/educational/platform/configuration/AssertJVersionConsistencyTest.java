package com.educational.platform.configuration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the AssertJ version declared in gradle/libs.versions.toml
 * is consistent with the AssertJ version transitively provided by
 * spring-boot-starter-test. The configuration module's test classpath
 * includes both:
 * <ul>
 *   <li>{@code testImplementation("org.springframework.boot", "spring-boot-starter-test")}
 *       — brings AssertJ transitively via the Spring Boot BOM</li>
 *   <li>The explicit {@code assertj} version key in libs.versions.toml
 *       which other modules may reference</li>
 * </ul>
 * A major version mismatch between the declared and transitive versions
 * would cause API incompatibility (e.g., removed assertion methods) or
 * classpath confusion. Complements {@link MockitoVersionConsistencyTest}
 * which performs the same validation for Mockito.
 */
class AssertJVersionConsistencyTest {

    private static String declaredAssertJVersion;

    @BeforeAll
    static void loadAssertJVersion() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        String tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));

        Matcher m = Pattern.compile("^assertj\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE)
                .matcher(tomlContent);
        assertThat(m.find())
                .as("libs.versions.toml must declare an 'assertj' version")
                .isTrue();
        declaredAssertJVersion = m.group(1);
    }

    @Test
    void declaredAssertJVersion_shouldFollowSemanticVersioning() {
        assertThat(declaredAssertJVersion)
                .as("Declared AssertJ version must follow semver (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+.*");
    }

    @Test
    void declaredAssertJVersion_shouldBeAssertJ3x() {
        assertThat(declaredAssertJVersion)
                .as("Declared AssertJ version must be 3.x to be compatible with "
                        + "Spring Boot 4.x's starter-test which expects AssertJ 3.x")
                .startsWith("3.");
    }

    @Test
    void runtimeAssertJ_shouldBeAvailable() {
        assertThat(Assertions.class.getPackage())
                .as("AssertJ must be resolvable at runtime — "
                        + "both starter-test and the version catalog should provide it")
                .isNotNull();
    }

    @Test
    void runtimeAssertJ_majorVersion_shouldMatchDeclaredMajor() {
        Package assertjPackage = Assertions.class.getPackage();
        String implVersion = assertjPackage.getImplementationVersion();
        if (implVersion != null) {
            String runtimeMajor = implVersion.split("\\.")[0];
            String declaredMajor = declaredAssertJVersion.split("\\.")[0];
            assertThat(runtimeMajor)
                    .as("Runtime AssertJ major version must match the declared major version — "
                            + "a major version mismatch between version catalog declaration and "
                            + "starter-test transitive could cause API incompatibility")
                    .isEqualTo(declaredMajor);
        }
    }

    @Test
    void declaredAssertJVersion_shouldNotBeOlderThan3_24() {
        String[] parts = declaredAssertJVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        assertThat(major * 100 + minor)
                .as("AssertJ version must be at least 3.24 for full Spring Boot 4.x compatibility — "
                        + "earlier versions lack assertion methods used by auto-configured test slices")
                .isGreaterThanOrEqualTo(324);
    }
}
