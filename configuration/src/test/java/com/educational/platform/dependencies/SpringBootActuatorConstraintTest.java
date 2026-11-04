package com.educational.platform.dependencies;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringBootVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringBootActuatorConstraintTest {

    private static final String ACTUATOR = "spring-boot-actuator-autoconfigure";

    @TempDir
    Path projectDir;

    @BeforeEach
    void setUp() throws IOException {
        Path root = Path.of(System.getProperty("rootProjectDir"));
        Files.createDirectories(projectDir.resolve("gradle"));
        Files.createDirectories(projectDir.resolve("probe"));
        Files.copy(root.resolve("build.gradle.kts"), projectDir.resolve("build.gradle.kts"));
        Files.copy(root.resolve("gradle/libs.versions.toml"), projectDir.resolve("gradle/libs.versions.toml"));
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'constraint-test'\ninclude 'probe'\n");
        Files.writeString(projectDir.resolve("gradle.properties"), "org.gradle.workers.max=2\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), """

                allprojects {
                    repositories {
                        clear()
                        maven { url = uri(rootProject.file("repository")) }
                    }
                }
                """, java.nio.file.StandardOpenOption.APPEND);

        for (String version : new String[]{"2.7.18", "3.0.13", "3.1.0", "3.1.4", "3.1.5-RC1", "3.1.5", "3.1.10", "3.2.0", "4.0.1"}) {
            publish(ACTUATOR, version, "");
        }
        publish("unrelated-library", "3.1.0", "");
        // A vulnerable BOM ensures that the current safe Boot BOM cannot mask a missing constraint.
        publish("spring-boot-dependencies", SpringBootVersion.getVersion(), """
                <dependencyManagement><dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-actuator-autoconfigure</artifactId>
                        <version>3.1.0</version>
                    </dependency>
                </dependencies></dependencyManagement>
                """);
        publish("actuator-carrier", "1.0", """
                <dependencies><dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-actuator-autoconfigure</artifactId>
                    <version>3.1.0</version>
                </dependency></dependencies>
                """);
    }

    @Test
    void implementation_rejectsVersionJustBelowSafeBoundary() throws IOException {
        writeProbe("implementation 'org.springframework.boot:" + ACTUATOR + ":3.1.4'", "compileClasspath");

        assertRejected(runner().buildAndFail());
    }

    @Test
    void runtimeOnly_rejectsVulnerableVersion() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:" + ACTUATOR + ":3.1.0'", "runtimeClasspath");

        assertRejected(runner().buildAndFail());
    }

    @Test
    void runtimeOnly_rejectsOlderMinorAndMajorVersions() throws IOException {
        for (String version : new String[]{"2.7.18", "3.0.13"}) {
            writeProbe("runtimeOnly 'org.springframework.boot:" + ACTUATOR + ":" + version + "'", "runtimeClasspath");

            assertRejected(runner().buildAndFail());
        }
    }

    @Test
    void runtimeOnly_rejectsPrereleaseOfMinimumSafeVersion() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:" + ACTUATOR + ":3.1.5-RC1'", "runtimeClasspath");

        assertRejected(runner().buildAndFail());
    }

    @Test
    void runtimeOnly_rejectsVulnerableTransitiveDependency() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:actuator-carrier:1.0'", "runtimeClasspath");

        assertRejected(runner().buildAndFail());
    }

    @Test
    void implementation_rejectsVulnerableBomManagedVersion() throws IOException {
        writeProbe("implementation 'org.springframework.boot:" + ACTUATOR + "'", "runtimeClasspath");

        assertRejected(runner().buildAndFail());
    }

    @Test
    void runtimeOnly_acceptsMinimumSafeVersion() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:" + ACTUATOR + ":3.1.5'", "runtimeClasspath");

        BuildResult result = runner().build();

        assertEquals(SUCCESS, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains(ACTUATOR + "-3.1.5.jar"), result.getOutput());
    }

    @Test
    void implementation_acceptsNewerMajorVersion() throws IOException {
        writeProbe("implementation 'org.springframework.boot:" + ACTUATOR + ":4.0.1'", "runtimeClasspath");

        BuildResult result = runner().build();

        assertEquals(SUCCESS, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains(ACTUATOR + "-4.0.1.jar"), result.getOutput());
    }

    @Test
    void runtimeOnly_acceptsMultiDigitPatchVersion() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:" + ACTUATOR + ":3.1.10'", "runtimeClasspath");

        BuildResult result = runner().build();

        assertEquals(SUCCESS, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains(ACTUATOR + "-3.1.10.jar"), result.getOutput());
    }

    @Test
    void runtimeOnly_acceptsNewerMinorVersion() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:" + ACTUATOR + ":3.2.0'", "runtimeClasspath");

        BuildResult result = runner().build();

        assertEquals(SUCCESS, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains(ACTUATOR + "-3.2.0.jar"), result.getOutput());
    }

    @Test
    void constraint_doesNotRejectOtherModules() throws IOException {
        writeProbe("runtimeOnly 'org.springframework.boot:unrelated-library:3.1.0'", "runtimeClasspath");

        BuildResult result = runner().build();

        assertEquals(SUCCESS, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains("Resolved artifacts: [unrelated-library-3.1.0.jar]"), result.getOutput());
    }

    @Test
    void constraint_doesNotIntroduceActuatorWhenAbsent() throws IOException {
        writeProbe("", "runtimeClasspath");

        BuildResult result = runner().build();

        assertEquals(SUCCESS, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains("Resolved artifacts: []"), result.getOutput());
    }

    private void assertRejected(BuildResult result) {
        assertEquals(FAILED, result.task(":probe:resolveActuator").getOutcome());
        assertTrue(result.getOutput().contains(ACTUATOR), result.getOutput());
        assertTrue(result.getOutput().contains("CVE-2023-20883"), result.getOutput());
    }

    private GradleRunner runner() {
        // Reuse the plugin cache populated by the enclosing build; fixture dependencies are all local.
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withArguments(":probe:resolveActuator", "--offline", "--console=plain", "--info",
                        "--gradle-user-home", System.getProperty("gradleUserHome"));
    }

    private void writeProbe(String dependency, String classpath) throws IOException {
        Files.writeString(projectDir.resolve("probe/build.gradle"), """
                dependencies { %s }
                tasks.register('resolveActuator') {
                    doLast {
                        println 'Resolved artifacts: ' + configurations.%s.files.collect { it.name }.sort()
                    }
                }
                """.formatted(dependency, classpath));
    }

    private void publish(String artifact, String version, String contents) throws IOException {
        Path directory = projectDir.resolve("repository/org/springframework/boot/" + artifact + "/" + version);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(artifact + "-" + version + ".pom"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(artifact, version, contents));
        try (var jar = new JarOutputStream(Files.newOutputStream(directory.resolve(artifact + "-" + version + ".jar")))) {
            jar.finish();
        }
    }
}
