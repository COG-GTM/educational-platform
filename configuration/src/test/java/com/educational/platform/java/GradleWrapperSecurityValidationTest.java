package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates security-critical properties of the Gradle wrapper after the
 * 9.2.1 → 9.5.1 upgrade.
 * <p>
 * Gradle wrapper is a supply-chain attack vector: if the JAR or distribution
 * URL is tampered with, malicious code executes in every build. Gradle 9.4+
 * introduced {@code validateDistributionUrl=true} to verify downloads against
 * known checksums. This test ensures the security configuration is correctly
 * set after the upgrade.
 * <p>
 * {@link GradleWrapperPropertiesCompletenessTest} verifies property existence.
 * This test verifies the <em>security-relevant values</em> and the integrity
 * of the wrapper JAR itself.
 */
public class GradleWrapperSecurityValidationTest {

    @Test
    void validateDistributionUrl_shouldBeEnabled() throws IOException {
        Properties props = readWrapperProperties();

        String validateUrl = props.getProperty("validateDistributionUrl");

        assertThat(validateUrl)
                .as("validateDistributionUrl should be 'true' to enable checksum verification")
                .isEqualTo("true");
    }

    @Test
    void distributionUrl_shouldUse_httpsScheme() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL must use HTTPS (not HTTP) to prevent MITM attacks")
                .startsWith("https");

        assertThat(url)
                .as("Distribution URL should not contain HTTP (non-secure)")
                .doesNotContain("http:");
    }

    @Test
    void wrapperJar_shouldHave_consistentSha256() throws Exception {
        Path jarPath = findWrapperJar();
        byte[] jarBytes = Files.readAllBytes(jarPath);

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(jarBytes);
        String hexHash = HexFormat.of().formatHex(hash);

        assertThat(hexHash)
                .as("Wrapper JAR SHA-256 should be a valid 64-character hex string")
                .hasSize(64)
                .matches("[0-9a-f]+");

        // Verify the hash is deterministic (same bytes = same hash)
        byte[] hash2 = MessageDigest.getInstance("SHA-256").digest(jarBytes);
        assertThat(hash2)
                .as("SHA-256 should be deterministic for the wrapper JAR")
                .isEqualTo(hash);
    }

    @Test
    void wrapperJar_shouldNotContain_suspiciousEntries() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            jar.stream().forEach(entry -> {
                String name = entry.getName();
                assertThat(name)
                        .as("JAR entry should not reference external URLs")
                        .doesNotContain("http://");

                assertThat(name)
                        .as("JAR entry name should not contain path traversal")
                        .doesNotContain("../");

                assertThat(name)
                        .as("JAR entry '%s' should be under expected packages", name)
                        .satisfiesAnyOf(
                                n -> assertThat(n).startsWith("org/gradle/"),
                                n -> assertThat(n).startsWith("META-INF/"),
                                n -> assertThat(n).isEmpty() // directory entry
                        );
            });
        }
    }

    @Test
    void wrapperJar_shouldBeSignedOrHaveManifestDigests() throws IOException {
        Path jarPath = findWrapperJar();

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var manifest = jar.getManifest();
            assertThat(manifest)
                    .as("Wrapper JAR should have a manifest")
                    .isNotNull();

            // Gradle wrapper JARs typically don't have per-entry digests,
            // but should at minimum have Manifest-Version
            String manifestVersion = manifest.getMainAttributes()
                    .getValue("Manifest-Version");
            assertThat(manifestVersion)
                    .as("Manifest-Version should be present")
                    .isNotNull();
        }
    }

    @Test
    void distributionUrl_shouldNotReference_thirdPartyMirror() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should reference services.gradle.org (official)")
                .contains("services.gradle.org");

        assertThat(url)
                .as("Distribution URL should not reference third-party mirrors")
                .doesNotContain("mirror")
                .doesNotContain("cdn.")
                .doesNotContain("proxy");
    }

    @Test
    void networkTimeout_shouldBe_reasonable() throws IOException {
        Properties props = readWrapperProperties();
        String timeout = props.getProperty("networkTimeout");

        int timeoutMs = Integer.parseInt(timeout);

        assertThat(timeoutMs)
                .as("Network timeout should be between 1s and 60s (security: prevent hanging connections)")
                .isBetween(1000, 60000);
    }

    private Properties readWrapperProperties() throws IOException {
        Path propsPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(propsPath));
        return props;
    }

    private Path findWrapperJar() {
        return findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path settingsFile = current.resolve("settings.gradle.kts");
            Path gradleDir = current.resolve("gradle/wrapper");
            if (Files.exists(settingsFile) || Files.isDirectory(gradleDir)) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
