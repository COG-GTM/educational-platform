package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the ordering of test dependencies in configuration/build.gradle.kts.
 * The PR added spring-boot-starter-test before the existing junit-jupiter-api
 * declaration. This ordering is significant:
 * <ul>
 *   <li>starter-test must appear first among test deps because it provides the
 *       comprehensive test infrastructure (Spring TestContext, MockMvc, etc.)
 *       that subsequent test libraries build upon</li>
 *   <li>JUnit and Mockito explicit declarations follow for version pinning
 *       where the project needs a specific version override</li>
 *   <li>ArchUnit is declared last as an optional architecture-testing tool</li>
 * </ul>
 * Complements {@link ConfigurationBuildFileStructuralOrderTest} (top-level block
 * ordering) and {@link BuildFileDependencyBlockStructureTest} (implementation
 * before test deps).
 */
class BuildFileTestDependencyOrderingTest {

    private static List<String> buildLines;

    @BeforeAll
    static void loadBuildFile() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        String content = Files.readString(dir.resolve("configuration/build.gradle.kts"));
        buildLines = content.lines().toList();
    }

    @Test
    void starterTest_shouldAppearBeforeJunitJupiter() {
        int starterTestLine = findFirstLineContaining("spring-boot-starter-test");
        int junitLine = findFirstLineContaining("junit-jupiter-api");

        assertThat(starterTestLine)
                .as("spring-boot-starter-test must be declared before junit-jupiter-api — "
                        + "starter-test provides the foundational test infrastructure "
                        + "that JUnit tests build upon")
                .isLessThan(junitLine);
    }

    @Test
    void starterTest_shouldAppearBeforeMockito() {
        int starterTestLine = findFirstLineContaining("spring-boot-starter-test");
        int mockitoLine = findFirstLineContaining("mockito-junit-jupiter");

        assertThat(starterTestLine)
                .as("spring-boot-starter-test must be declared before mockito-junit-jupiter — "
                        + "starter-test brings its own Mockito transitive; explicit Mockito "
                        + "follows for version pinning")
                .isLessThan(mockitoLine);
    }

    @Test
    void starterTest_shouldAppearBeforeArchUnit() {
        int starterTestLine = findFirstLineContaining("spring-boot-starter-test");
        int archunitLine = findFirstLineContaining("archunit-junit5");

        assertThat(starterTestLine)
                .as("spring-boot-starter-test must be declared before archunit-junit5")
                .isLessThan(archunitLine);
    }

    @Test
    void starterTest_shouldBeFirstTestImplementationDependency() {
        int firstTestImplLine = -1;
        for (int i = 0; i < buildLines.size(); i++) {
            if (buildLines.get(i).trim().startsWith("testImplementation(")) {
                firstTestImplLine = i;
                break;
            }
        }
        assertThat(firstTestImplLine)
                .as("At least one testImplementation dependency must exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(buildLines.get(firstTestImplLine))
                .as("The first testImplementation dependency must be spring-boot-starter-test — "
                        + "it provides the broadest test infrastructure and should precede "
                        + "individual test library declarations")
                .contains("spring-boot-starter-test");
    }

    @Test
    void junitPlatformDeps_shouldAppearAfterJunitJupiter() {
        int jupiterLine = findFirstLineContaining("junit-jupiter-api");
        int platformEngineLine = findFirstLineContaining("junit-platform-engine");
        int platformLauncherLine = findFirstLineContaining("junit-platform-launcher");

        assertThat(platformEngineLine)
                .as("junit-platform-engine should follow junit-jupiter-api")
                .isGreaterThan(jupiterLine);
        assertThat(platformLauncherLine)
                .as("junit-platform-launcher should follow junit-jupiter-api")
                .isGreaterThan(jupiterLine);
    }

    private int findFirstLineContaining(String substring) {
        for (int i = 0; i < buildLines.size(); i++) {
            String line = buildLines.get(i).trim();
            if (!line.startsWith("//") && line.contains(substring)) {
                return i;
            }
        }
        return -1;
    }
}
