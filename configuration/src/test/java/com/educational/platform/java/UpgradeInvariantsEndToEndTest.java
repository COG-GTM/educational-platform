package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end invariant checks that tie together ALL artifacts affected by the
 * Java 25 → 26 upgrade into a single, comprehensive validation.
 * <p>
 * Unlike {@link VersionUpgradeAtomicCoherenceTest} which validates version
 * fingerprints, this test validates <em>functional invariants</em>:
 * <ul>
 *   <li>The bytecode version produced by the compiler matches the runtime</li>
 *   <li>The ArchUnit version can parse the produced bytecode</li>
 *   <li>The Gradle version supports the configured Java target</li>
 *   <li>The README documents the same Java version the build uses</li>
 *   <li>New test dependencies are actually usable (not just declared)</li>
 * </ul>
 */
public class UpgradeInvariantsEndToEndTest {

    @Test
    void invariant_compilerOutput_matchesRuntime() throws IOException {
        int runtimeFeature = Runtime.version().feature();
        int bytecodeMajor = readOwnBytecodeMajorVersion();
        int derivedJava = bytecodeMajor - 44;

        assertThat(derivedJava)
                .as("Bytecode-derived Java version (%d - 44 = %d) should match runtime feature (%d)",
                        bytecodeMajor, derivedJava, runtimeFeature)
                .isEqualTo(runtimeFeature);
    }

    @Test
    void invariant_buildTarget_matchesRuntime() throws IOException {
        int runtimeFeature = Runtime.version().feature();
        int buildTarget = parseBuildJavaVersion();

        assertThat(buildTarget)
                .as("build.gradle.kts Java target should match JVM runtime feature version")
                .isEqualTo(runtimeFeature);
    }

    @Test
    void invariant_readmeVersion_matchesBuildTarget() throws IOException {
        int buildTarget = parseBuildJavaVersion();
        String readme = Files.readString(findProjectRoot().resolve("README.md"));

        assertThat(readme)
                .as("README should reference the same Java version as build target (%d)", buildTarget)
                .contains("Java " + buildTarget);

        // Previous version should not be mentioned
        int previousVersion = buildTarget - 1;
        assertThat(readme)
                .as("README should not reference previous Java version (%d)", previousVersion)
                .doesNotContain("Java " + previousVersion);
    }

    @Test
    void invariant_gradleSupports_configuredJavaTarget() throws IOException {
        int buildTarget = parseBuildJavaVersion();
        int[] gradleVersion = parseGradleVersion();

        // Java 26 requires Gradle >= 9.4.0
        if (buildTarget >= 26) {
            boolean compatible = gradleVersion[0] > 9
                    || (gradleVersion[0] == 9 && gradleVersion[1] >= 4);

            assertThat(compatible)
                    .as("Gradle %d.%d.%d should support Java %d (requires >= 9.4.0)",
                            gradleVersion[0], gradleVersion[1], gradleVersion[2], buildTarget)
                    .isTrue();
        }
    }

    @Test
    void invariant_archunitCanParse_producedBytecode() throws IOException {
        String catalogVersion = extractCatalogVersion("archunit");
        int bytecodeMajor = readOwnBytecodeMajorVersion();

        // ArchUnit 1.4.2+ supports class file major version 70
        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher m = semver.matcher(catalogVersion);
        assertThat(m.find()).isTrue();
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = Integer.parseInt(m.group(3));

        if (bytecodeMajor >= 70) {
            boolean supported = major > 1
                    || (major == 1 && minor > 4)
                    || (major == 1 && minor == 4 && patch >= 2);

            assertThat(supported)
                    .as("ArchUnit %s should support bytecode major version %d",
                            catalogVersion, bytecodeMajor)
                    .isTrue();
        }
    }

    @Test
    void invariant_newTestDeps_areUsable() {
        // junit-jupiter-params: verify @ParameterizedTest annotation is loadable
        assertThat(org.junit.jupiter.params.ParameterizedTest.class)
                .as("junit-jupiter-params should be on the classpath")
                .isNotNull();

        // assertj-core: verify Assertions class is loadable
        assertThat(org.assertj.core.api.Assertions.class)
                .as("assertj-core should be on the classpath")
                .isNotNull();

        // junit-jupiter-engine: verify JupiterTestEngine is discoverable
        var engines = java.util.ServiceLoader.load(
                org.junit.platform.engine.TestEngine.class);
        boolean jupiterFound = false;
        for (var engine : engines) {
            if (engine.getId().equals("junit-jupiter")) {
                jupiterFound = true;
                break;
            }
        }
        assertThat(jupiterFound)
                .as("JUnit Jupiter engine should be discoverable via ServiceLoader")
                .isTrue();
    }

    @Test
    void invariant_sourceAndTargetCompatibility_areIdentical() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));
        Pattern srcPat = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Pattern tgtPat = Pattern.compile("targetCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");

        Matcher srcM = srcPat.matcher(content);
        Matcher tgtM = tgtPat.matcher(content);

        assertThat(srcM.find()).isTrue();
        assertThat(tgtM.find()).isTrue();

        assertThat(srcM.group(1))
                .as("sourceCompatibility and targetCompatibility should be identical")
                .isEqualTo(tgtM.group(1));
    }

    private int parseBuildJavaVersion() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));
        Matcher m = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)")
                .matcher(content);
        assertThat(m.find()).isTrue();
        return Integer.parseInt(m.group(1));
    }

    private int[] parseGradleVersion() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = props.getProperty("distributionUrl");
        Matcher m = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)").matcher(distUrl);
        assertThat(m.find()).isTrue();
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
        };
    }

    private String extractCatalogVersion(String key) throws IOException {
        String content = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
        Matcher m = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"").matcher(content);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private int readOwnBytecodeMajorVersion() throws IOException {
        String resource = "/com/educational/platform/java/UpgradeInvariantsEndToEndTest.class";
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            assertThat(is).isNotNull();
            DataInputStream dis = new DataInputStream(is);
            dis.readInt();           // magic
            dis.readUnsignedShort(); // minor
            return dis.readUnsignedShort(); // major
        }
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))
                    || Files.isDirectory(current.resolve("gradle/wrapper"))) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
