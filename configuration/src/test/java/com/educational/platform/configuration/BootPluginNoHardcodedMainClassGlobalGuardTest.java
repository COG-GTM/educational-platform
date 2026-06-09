package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against hardcoded {@code mainClass} / {@code mainClassName}
 * declarations in ANY module's build file across the entire project.
 * {@link BootPluginTaskRegistrationTest} validates the configuration module
 * specifically does not set {@code mainClass}; this test extends that guard
 * to ALL modules, preventing any build file from bypassing the Spring Boot
 * plugin's auto-detection of the main class via {@code @SpringBootApplication}.
 * <p>
 * A hardcoded main class in any module would create a desynchronization
 * risk: if the application class is renamed or moved, the hardcoded value
 * would silently point to a non-existent class, causing bootRun to fail
 * at startup rather than at build time.
 */
class BootPluginNoHardcodedMainClassGlobalGuardTest {

    private static final List<Path> allBuildFiles = new ArrayList<>();

    @BeforeAll
    static void findAllBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();

        Path root = dir;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().equals("build.gradle.kts")) {
                    allBuildFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                if (dirName.equals(".gradle") || dirName.equals("build") || dirName.equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertThat(allBuildFiles)
                .as("At least the root and configuration build files must be found")
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void noBuildFile_shouldHardcodeMainClass() throws IOException {
        for (Path buildFile : allBuildFiles) {
            String content = Files.readString(buildFile);
            assertThat(content)
                    .as("Build file %s must NOT hardcode mainClass — "
                            + "the Spring Boot plugin auto-detects it from @SpringBootApplication",
                            buildFile)
                    .doesNotContainPattern("mainClass\\s*[.=]")
                    .doesNotContain("mainClassName");
        }
    }

    @Test
    void noBuildFile_shouldSetStartClass() throws IOException {
        for (Path buildFile : allBuildFiles) {
            String content = Files.readString(buildFile);
            assertThat(content)
                    .as("Build file %s must NOT set Start-Class — "
                            + "the Spring Boot plugin sets this manifest attribute automatically",
                            buildFile)
                    .doesNotContain("Start-Class");
        }
    }

    @Test
    void noBuildFile_shouldSetMainClassInManifest() throws IOException {
        for (Path buildFile : allBuildFiles) {
            String content = Files.readString(buildFile);
            assertThat(content)
                    .as("Build file %s must NOT set Main-Class in manifest — "
                            + "the Spring Boot plugin manages MANIFEST.MF via bootJar",
                            buildFile)
                    .doesNotContain("Main-Class");
        }
    }
}
