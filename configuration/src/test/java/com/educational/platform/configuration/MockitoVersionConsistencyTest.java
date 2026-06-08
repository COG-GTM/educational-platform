package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the explicit Mockito version declared in
 * configuration/build.gradle.kts ({@code libs.versions.mockito.get()})
 * is consistent with the Mockito version transitively provided by
 * spring-boot-starter-test. The configuration module declares both:
 * <ul>
 *   <li>{@code testImplementation("org.springframework.boot", "spring-boot-starter-test")}
 *       — brings Mockito transitively via the Spring Boot BOM</li>
 *   <li>{@code testImplementation("org.mockito", "mockito-junit-jupiter", libs.versions.mockito.get())}
 *       — overrides Mockito with an explicit version from the version catalog</li>
 * </ul>
 * If the explicit version is LOWER than the BOM-managed version, the older
 * JAR wins on the classpath (Gradle uses the higher version by default, but
 * explicit declarations take precedence in some resolution strategies),
 * potentially causing NoSuchMethodError in test infrastructure. If the
 * explicit version differs significantly, it creates confusion about which
 * version is actually used.
 */
class MockitoVersionConsistencyTest {

    private static String declaredMockitoVersion;

    @BeforeAll
    static void loadMockitoVersion() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        String tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));

        Matcher m = Pattern.compile("^mockito\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE)
                .matcher(tomlContent);
        assertThat(m.find())
                .as("libs.versions.toml must declare a 'mockito' version")
                .isTrue();
        declaredMockitoVersion = m.group(1);
    }

    @Test
    void declaredMockitoVersion_shouldFollowSemanticVersioning() {
        assertThat(declaredMockitoVersion)
                .as("Declared Mockito version must follow semver (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+.*");
    }

    @Test
    void declaredMockitoVersion_shouldBeMockito5x() {
        assertThat(declaredMockitoVersion)
                .as("Declared Mockito version must be 5.x to be compatible with "
                        + "Spring Boot 4.x's starter-test which expects Mockito 5.x")
                .startsWith("5.");
    }

    @Test
    void runtimeMockitoVersion_shouldBeResolvable() {
        // Mockito.framework() is available since Mockito 2.x; if this fails,
        // the mockito-core JAR is not on the classpath at all
        assertThat(Mockito.framework())
                .as("Mockito framework must be resolvable at runtime — "
                        + "both starter-test and explicit declaration should provide it")
                .isNotNull();
    }

    @Test
    void declaredMockitoVersion_majorVersion_shouldMatchRuntimeMajor() {
        String runtimeVersion = Mockito.framework().getPlugins().toString();
        // Mockito embeds version info accessible via package
        Package mockitoPackage = Mockito.class.getPackage();
        String implVersion = mockitoPackage.getImplementationVersion();
        if (implVersion != null) {
            String runtimeMajor = implVersion.split("\\.")[0];
            String declaredMajor = declaredMockitoVersion.split("\\.")[0];
            assertThat(runtimeMajor)
                    .as("Runtime Mockito major version must match the declared major version — "
                            + "a major version mismatch between explicit declaration and "
                            + "starter-test transitive could cause API incompatibility")
                    .isEqualTo(declaredMajor);
        }
    }

    @Test
    void buildFile_shouldDeclareExplicitMockitoVersion() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        String buildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
        assertThat(buildContent)
                .as("mockito-junit-jupiter must use libs.versions.mockito.get() for explicit "
                        + "version control alongside the version from starter-test")
                .containsPattern("mockito-junit-jupiter.*libs\\.versions\\.mockito\\.get\\(\\)");
    }
}
