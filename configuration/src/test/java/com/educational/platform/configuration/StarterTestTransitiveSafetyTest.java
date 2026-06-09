package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates safety constraints for spring-boot-starter-test transitive
 * dependencies. The PR added starter-test to configuration/build.gradle.kts;
 * these tests guard against classpath pollution that could cause subtle
 * test failures or production classpath leaks:
 * <ul>
 *   <li>DevTools must NOT be on the test classpath — its classloader
 *       restart behavior causes spurious ClassCastExceptions in tests</li>
 *   <li>Selenium/HtmlUnit must NOT be pulled in transitively — they are
 *       not needed and add heavyweight browser dependencies</li>
 *   <li>JUnit 4 vintage engine should NOT be present — the project uses
 *       JUnit 5 exclusively and vintage creates ambiguous test discovery</li>
 * </ul>
 */
class StarterTestTransitiveSafetyTest {

    @Test
    void devTools_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.boot.devtools.restart.Restarter"))
                .as("spring-boot-devtools Restarter must NOT be on the test classpath — "
                        + "DevTools classloader restart causes ClassCastException in tests "
                        + "and should only be present via developmentOnly scope")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void devToolsAutoConfiguration_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.devtools.autoconfigure.DevToolsProperties"))
                .as("DevToolsProperties must NOT be on the test classpath — "
                        + "DevTools auto-configuration would interfere with test isolation")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void seleniumWebDriver_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.openqa.selenium.WebDriver"))
                .as("Selenium WebDriver must NOT be on the test classpath — "
                        + "the project does not use browser-based testing via starter-test")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void htmlUnit_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.htmlunit.WebClient"))
                .as("HtmlUnit must NOT be on the test classpath — "
                        + "heavyweight browser simulation is not needed for this project")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void junitVintageEngine_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.junit.vintage.engine.VintageTestEngine"))
                .as("JUnit Vintage engine must NOT be on the test classpath — "
                        + "the project uses JUnit 5 exclusively; vintage would create "
                        + "ambiguous test discovery with JUnit 4 compatibility")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void junit4_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.junit.Test"))
                .as("JUnit 4 @Test annotation must NOT be on the test classpath — "
                        + "only JUnit 5 (org.junit.jupiter.api.Test) should be used")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void springBootTestAutoConfiguration_shouldBeOnTestClasspath() {
        // Positive check: starter-test's core auto-configuration support must be present
        try {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration");
            assertThat(clazz.isAnnotation())
                    .as("@OverrideAutoConfiguration must be a resolvable annotation for test slice support")
                    .isTrue();
        } catch (ClassNotFoundException e) {
            assertThat(false)
                    .as("@OverrideAutoConfiguration must be on test classpath via starter-test")
                    .isTrue();
        }
    }
}
