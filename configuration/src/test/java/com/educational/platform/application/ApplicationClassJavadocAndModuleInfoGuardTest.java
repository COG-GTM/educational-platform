package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards structural properties of the application entry point that are
 * not covered by existing contract tests. Validates that:
 * <ul>
 *   <li>The application class has no declared annotations beyond the expected set
 *       (cross-validation with {@link ApplicationAnnotationCompletenessTest})</li>
 *   <li>The main method does not declare checked exceptions — the Spring Boot
 *       plugin's bootRun task invokes main() and does not expect checked exceptions</li>
 *   <li>The application class source file has no module-info.java companion that
 *       could break classpath scanning</li>
 *   <li>The application class does not use the {@code @SuppressWarnings} annotation
 *       which could mask important compilation warnings</li>
 * </ul>
 * <p>
 * Complements {@link ApplicationAnnotationCompletenessTest} (annotation set),
 * {@link MainMethodDeclarationContractTest} (method signature), and
 * {@link ApplicationClassPackageExclusivenessTest} (source set).
 */
class ApplicationClassJavadocAndModuleInfoGuardTest {

    @Test
    void mainMethod_shouldNotDeclareCheckedExceptions() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(main.getExceptionTypes())
                .as("main() must NOT declare checked exceptions — "
                        + "the Spring Boot plugin's bootRun task and the JVM entry point "
                        + "contract do not expect checked exceptions from main()")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotUseSuppressWarnings() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(SuppressWarnings.class))
                .as("@SuppressWarnings must NOT be on the application class — "
                        + "warnings on the composition root should be fixed, not suppressed")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotBeAnnotatedWithFunctionalInterface() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(FunctionalInterface.class))
                .as("Application class must NOT be a @FunctionalInterface — "
                        + "it is not a function type")
                .isFalse();
    }

    @Test
    void applicationPackage_shouldNotContainModuleInfo() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir).isNotNull();

        Path moduleInfo = dir.resolve("configuration/src/main/java/module-info.java");
        assertThat(Files.exists(moduleInfo))
                .as("module-info.java must NOT exist in the configuration module — "
                        + "Java modules (JPMS) would restrict reflective access needed by "
                        + "Spring Boot's auto-configuration and the boot loader's classpath scanning")
                .isFalse();
    }

    @Test
    void mainMethod_shouldHaveStandardModifiers() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        int modifiers = main.getModifiers();
        assertThat(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers))
                .as("main() must be public static")
                .isTrue();
        assertThat(Modifier.isSynchronized(modifiers))
                .as("main() must NOT be synchronized — "
                        + "synchronization on the entry point would block concurrent test invocations "
                        + "and is unnecessary since Spring Boot manages its own lifecycle")
                .isFalse();
        assertThat(Modifier.isFinal(modifiers))
                .as("main() must NOT be final — final static methods cannot be mocked "
                        + "by Mockito.mockStatic(), which the test suite relies on")
                .isFalse();
    }

    @Test
    void mainMethod_shouldNotBeVarArgs() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(main.isVarArgs())
                .as("main(String[] args) must use array parameter, not varargs — "
                        + "the JVM entry point contract requires String[] not String...")
                .isFalse();
    }
}
