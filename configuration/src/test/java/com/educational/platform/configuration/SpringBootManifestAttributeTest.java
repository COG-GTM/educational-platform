package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structural requirements for the Spring Boot plugin's bootJar
 * packaging. The plugin repackages the JAR with a manifest that references
 * the application's main class; these tests ensure the entry point meets
 * all requirements for a valid Start-Class manifest entry.
 */
class SpringBootManifestAttributeTest {

    @Test
    void mainClass_fullyQualifiedName_shouldMatchBootJarStartClass() {
        // The Spring Boot plugin configures bootJar to use this class as Start-Class
        assertThat(EducationalPlatformApplication.class.getName())
                .as("The application's FQN must be stable for the bootJar manifest Start-Class attribute")
                .isEqualTo("com.educational.platform.EducationalPlatformApplication");
    }

    @Test
    void mainClass_shouldBeInExpectedPackage_forBootJarManifest() {
        assertThat(EducationalPlatformApplication.class.getPackageName())
                .as("Application class package must be the root package for proper class loading in bootJar")
                .isEqualTo("com.educational.platform");
    }

    @Test
    void mainMethod_shouldBeCallableViaReflection_forBootJarLauncher() throws Exception {
        // The boot loader invokes main() via reflection; verify it is accessible
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(main.canAccess(null))
                .as("main() must be accessible via reflection for the boot loader")
                .isTrue();
    }

    @Test
    void mainMethod_shouldHaveVarArgsCompatibleSignature() throws Exception {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(main.getParameterTypes()[0].isArray())
                .as("main() parameter must be an array type for JVM entry point contract")
                .isTrue();
        assertThat(main.getParameterTypes()[0].getComponentType())
                .as("main() parameter component type must be String")
                .isEqualTo(String.class);
    }

    @Test
    void applicationClass_shouldNotDeclareMultipleMainMethods() {
        long mainMethodCount = java.util.Arrays.stream(EducationalPlatformApplication.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("main"))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .count();
        assertThat(mainMethodCount)
                .as("Application class must have exactly one public static main method for unambiguous bootJar entry")
                .isEqualTo(1);
    }

    @Test
    void springBootVersion_shouldBeResolvable_forManifestAttribute() {
        // bootJar writes Spring-Boot-Version to the manifest; verify it's available
        assertThat(SpringBootVersion.getVersion())
                .as("Spring Boot version must be resolvable for manifest Spring-Boot-Version attribute")
                .isNotNull()
                .isNotBlank();
    }
}
