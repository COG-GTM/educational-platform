package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against applying the WAR plugin alongside the Spring Boot plugin
 * in the configuration module. The project uses an embedded Tomcat server
 * via spring-boot-starter-web; applying the WAR plugin would register
 * a {@code bootWar} task that conflicts with {@code bootJar} and produce
 * a deployable WAR archive incompatible with the embedded-server
 * architecture. Additionally, the WAR plugin would re-enable the standard
 * {@code war} task, creating duplicate packaging artifacts.
 * <p>
 * Complements {@link BootPluginJarTaskConflictTest} (which guards against
 * the {@code application} plugin, explicit {@code jar} task configuration,
 * and distribution configuration) and {@link ConflictingPluginExclusionGuardTest}
 * (which guards against other conflicting plugins). This test specifically
 * validates that WAR packaging is excluded across all module build files.
 */
class BootPluginWarPackagingExclusionTest {

    private static Path projectRoot;
    private static String configBuildContent;

    @BeforeAll
    static void loadBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        configBuildContent = Files.readString(
                dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void configurationBuildFile_shouldNotApplyWarPlugin() {
        assertThat(configBuildContent)
                .as("configuration/build.gradle.kts must NOT apply the 'war' plugin — "
                        + "the project uses embedded Tomcat via starter-web; WAR packaging "
                        + "would conflict with bootJar and the embedded-server architecture")
                .doesNotContain("\"war\"")
                .doesNotContainPattern("plugin.*war");
    }

    @Test
    void configurationBuildFile_shouldNotConfigureBootWarTask() {
        assertThat(configBuildContent)
                .as("configuration/build.gradle.kts must NOT configure a bootWar task — "
                        + "bootWar is registered by the Spring Boot plugin only when the WAR "
                        + "plugin is also applied; its presence indicates a WAR plugin leak")
                .doesNotContainPattern("bootWar\\s*\\{");
    }

    @Test
    void configurationBuildFile_shouldNotConfigureProvidedRuntime() {
        assertThat(configBuildContent)
                .as("configuration/build.gradle.kts must NOT use providedRuntime — "
                        + "this dependency scope is only available with the WAR plugin "
                        + "and is used for external servlet containers, not embedded Tomcat")
                .doesNotContain("providedRuntime");
    }

    @Test
    void rootBuildFile_shouldNotApplyWarPluginToSubprojects() throws IOException {
        String rootBuildContent = Files.readString(
                projectRoot.resolve("build.gradle.kts"));
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts must NOT apply the 'war' plugin to subprojects — "
                        + "WAR packaging is not part of the deployment strategy")
                .doesNotContain("\"war\"");
    }

    @Test
    void noModuleBuildFile_shouldApplyWarPlugin() throws IOException {
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .toList();
            for (Path buildFile : buildFiles) {
                String content = Files.readString(buildFile);
                List<String> warLines = content.lines()
                        .filter(line -> !line.trim().startsWith("//"))
                        .filter(line -> line.contains("\"war\"") || line.contains("bootWar"))
                        .toList();
                assertThat(warLines)
                        .as("Build file '%s' must NOT reference WAR plugin or bootWar task",
                                projectRoot.relativize(buildFile))
                        .isEmpty();
            }
        }
    }
}
