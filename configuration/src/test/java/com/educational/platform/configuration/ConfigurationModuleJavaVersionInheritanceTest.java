package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the configuration module does NOT override the Java version
 * configuration inherited from the root build.gradle.kts. The root build sets
 * sourceCompatibility and targetCompatibility to Java 25 in the subprojects
 * block; if the configuration module re-declares these or uses a Java toolchain
 * block, it could compile with a different Java level than other modules,
 * causing bytecode incompatibilities at runtime when bootRun loads classes
 * from all modules into a single JVM.
 * <p>
 * Complements {@link RootBuildJavaVersionCompatibilityTest} which validates
 * the root-level Java version declaration.
 */
class ConfigurationModuleJavaVersionInheritanceTest {

    private static String buildContent;

    @BeforeAll
    static void loadBuildFile() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        buildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void buildFile_shouldNotOverrideSourceCompatibility() {
        assertThat(buildContent)
                .as("configuration module must NOT set sourceCompatibility — "
                        + "it inherits Java 25 from the root subprojects block; "
                        + "overriding would risk bytecode version mismatches with "
                        + "other modules loaded by bootRun")
                .doesNotContain("sourceCompatibility");
    }

    @Test
    void buildFile_shouldNotOverrideTargetCompatibility() {
        assertThat(buildContent)
                .as("configuration module must NOT set targetCompatibility — "
                        + "it inherits from the root subprojects block")
                .doesNotContain("targetCompatibility");
    }

    @Test
    void buildFile_shouldNotDeclareJavaToolchain() {
        assertThat(buildContent)
                .as("configuration module must NOT declare a java toolchain block — "
                        + "toolchains override sourceCompatibility/targetCompatibility "
                        + "and could cause the module to compile against a different "
                        + "JDK than other modules")
                .doesNotContainPattern("java\\s*\\{")
                .doesNotContain("toolchain");
    }

    @Test
    void buildFile_shouldNotSetJvmTarget() {
        assertThat(buildContent)
                .as("configuration module must NOT set jvmTarget — "
                        + "this is a Kotlin DSL property and should not appear in a Java module")
                .doesNotContain("jvmTarget");
    }

    @Test
    void buildFile_shouldNotSetRelease() {
        assertThat(buildContent)
                .as("configuration module must NOT set the --release compiler option — "
                        + "Java version is centrally managed in the root build")
                .doesNotContainPattern("release\\s*[.=]");
    }
}
