package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that JUnit Platform correctly discovers and plans tests on
 * Java 26 compiled classes using the ServiceLoader-based engine discovery.
 * <p>
 * The PR added {@code junit-jupiter-engine} as a {@code testRuntimeOnly}
 * dependency and {@code junit-platform-launcher} as {@code testImplementation}.
 * This test validates the full discovery pipeline:
 * <ol>
 *   <li>ServiceLoader finds the JupiterTestEngine on the classpath</li>
 *   <li>LauncherFactory creates a Launcher instance</li>
 *   <li>The Launcher discovers test descriptors from Java 26 bytecode</li>
 *   <li>The test plan contains expected test classes</li>
 * </ol>
 * Without a functional engine discovery pipeline, no tests would run at all,
 * making this a critical smoke test for the test infrastructure.
 */
public class JUnitPlatformDiscoveryJava26Test {

    @Test
    void serviceLoader_shouldDiscover_jupiterTestEngine() {
        ServiceLoader<TestEngine> loader = ServiceLoader.load(TestEngine.class);
        long engineCount = StreamSupport.stream(loader.spliterator(), false).count();

        assertThat(engineCount)
                .as("ServiceLoader should discover at least one TestEngine (JupiterTestEngine)")
                .isGreaterThan(0);
    }

    @Test
    void serviceLoader_shouldFindEngine_withJupiterId() {
        ServiceLoader<TestEngine> loader = ServiceLoader.load(TestEngine.class);
        boolean hasJupiter = StreamSupport.stream(loader.spliterator(), false)
                .anyMatch(engine -> "junit-jupiter".equals(engine.getId()));

        assertThat(hasJupiter)
                .as("ServiceLoader should find an engine with id 'junit-jupiter'")
                .isTrue();
    }

    @Test
    void launcherFactory_shouldCreateLauncher_onJava26() {
        assertThatCode(() -> {
            Launcher launcher = LauncherFactory.create();
            assertThat(launcher).isNotNull();
        }).as("LauncherFactory should create a Launcher instance on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void launcher_shouldDiscoverTests_fromJava26CompiledClass() {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(JUnitPlatformDiscoveryJava26Test.class))
                .build();

        TestPlan testPlan = launcher.discover(request);

        assertThat(testPlan.containsTests())
                .as("TestPlan should contain tests discovered from this Java 26 compiled class")
                .isTrue();

        assertThat(testPlan.getRoots())
                .as("TestPlan should have root descriptors")
                .isNotEmpty();
    }

    @Test
    void launcher_shouldDiscoverTests_fromProductionTestClass() {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(JavaVersionTest.class))
                .build();

        TestPlan testPlan = launcher.discover(request);

        assertThat(testPlan.containsTests())
                .as("TestPlan should discover tests from JavaVersionTest (Java 26 compiled)")
                .isTrue();
    }

    @Test
    void launcher_shouldDiscoverTests_byPackage() {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("com.educational.platform.java"))
                .build();

        TestPlan testPlan = launcher.discover(request);

        assertThat(testPlan.containsTests())
                .as("TestPlan should discover tests from the java test package")
                .isTrue();

        long testCount = testPlan.countTestIdentifiers(id -> id.isTest());

        assertThat(testCount)
                .as("TestPlan should find multiple tests in the package")
                .isGreaterThan(10);
    }

    @Test
    void testEngine_groupId_shouldBeAccessibleViaReflection() {
        ServiceLoader<TestEngine> loader = ServiceLoader.load(TestEngine.class);
        TestEngine jupiterEngine = StreamSupport.stream(loader.spliterator(), false)
                .filter(engine -> "junit-jupiter".equals(engine.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(jupiterEngine.getGroupId())
                .as("JupiterTestEngine groupId should be org.junit.jupiter")
                .hasValue("org.junit.jupiter");
    }
}
