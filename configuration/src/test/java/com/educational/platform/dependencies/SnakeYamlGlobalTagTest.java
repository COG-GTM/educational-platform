package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2022-1471: snakeyaml < 2.0 instantiates arbitrary classes from global tags.
 */
class SnakeYamlGlobalTagTest {

    @Test
    void runtimeSnakeYamlVersion_isAtLeastTwo() throws IOException {
        // given
        Properties properties = new Properties();
        try (var resource = Yaml.class.getResourceAsStream("/META-INF/maven/org.yaml/snakeyaml/pom.properties")) {
            assertNotNull(resource, "SnakeYAML jar must provide Maven version metadata");
            properties.load(resource);
        }

        // when
        String version = properties.getProperty("version");

        // then
        assertNotNull(version, "SnakeYAML Maven metadata must contain a version");
        assertTrue(Integer.parseInt(version.split("\\.")[0]) >= 2,
                "Runtime SnakeYAML must be >= 2.0 (CVE-2022-1471), got: " + version);
    }

    @Test
    void defaultYamlLoader_loadsLiquibaseMasterChangelog() throws IOException {
        // given
        Yaml yaml = new Yaml();

        // when
        Object document;
        try (var resource = getClass().getResourceAsStream("/db/changelog/db.changelog-master.yml")) {
            assertNotNull(resource, "Application Liquibase master changelog must be on the test classpath");
            document = yaml.load(resource);
        }

        // then
        Map<?, ?> changelog = assertInstanceOf(Map.class, document);
        List<?> changes = assertInstanceOf(List.class, changelog.get("databaseChangeLog"));
        assertFalse(changes.isEmpty(), "Liquibase master changelog must include module changelogs");
        for (Object change : changes) {
            Map<?, ?> entry = assertInstanceOf(Map.class, change);
            Map<?, ?> include = assertInstanceOf(Map.class, entry.get("include"));
            String file = assertInstanceOf(String.class, include.get("file"));
            assertTrue(file.startsWith("classpath*:/db/"), "Expected a module changelog path, got: " + file);
        }
    }

    @Test
    void defaultYamlLoader_acceptsStandardScalarTags() {
        // given
        Yaml yaml = new Yaml();
        String document = "[!!str 42, !!int '42', !!bool true, !!null null]";

        // when
        Object result = yaml.load(document);

        // then
        List<?> values = assertInstanceOf(List.class, result);
        assertEquals(Arrays.asList("42", 42, true, null), values);
    }

    @Test
    void defaultYamlLoader_rejectsGlobalTags() {
        // given
        Yaml yaml = new Yaml();
        String document = "!!java.net.URI [\"https://example.com\"]";

        // when
        YAMLException exception = assertThrows(YAMLException.class, () -> yaml.load(document));

        // then
        assertTrue(exception.getMessage().contains("Global tag is not allowed"),
                "snakeyaml must be >= 2.0 (CVE-2022-1471), got: " + exception.getMessage());
    }

    @Test
    void includedModuleChangelogs_resolveOnClasspathAndLoadWithoutGlobalTags() throws IOException {
        // given
        Yaml yaml = new Yaml();
        List<?> includes;
        try (var resource = getClass().getResourceAsStream("/db/changelog/db.changelog-master.yml")) {
            assertNotNull(resource, "Application Liquibase master changelog must be on the test classpath");
            Map<?, ?> master = assertInstanceOf(Map.class, yaml.load(resource));
            includes = assertInstanceOf(List.class, master.get("databaseChangeLog"));
        }

        for (Object include : includes) {
            Map<?, ?> includeEntry = assertInstanceOf(Map.class, ((Map<?, ?>) include).get("include"));
            String path = ((String) includeEntry.get("file")).substring("classpath*:".length());

            // when
            Object document;
            try (var resource = getClass().getResourceAsStream(path)) {
                assertNotNull(resource, "Included module changelog must be on the classpath: " + path);
                document = yaml.load(resource);
            }

            // then
            Map<?, ?> changelog = assertInstanceOf(Map.class, document, path);
            List<?> changeSets = assertInstanceOf(List.class, changelog.get("databaseChangeLog"), path);
            assertFalse(changeSets.isEmpty(), "Module changelog must contain change sets: " + path);
        }
    }

    @Test
    void explicitNonSafeConstructor_rejectsGlobalTags() {
        // given
        Yaml yaml = new Yaml(new Constructor(new LoaderOptions()));
        String document = "!!java.net.URI [\"https://example.com\"]";

        // when
        YAMLException exception = assertThrows(YAMLException.class, () -> yaml.load(document));

        // then
        assertTrue(exception.getMessage().contains("Global tag is not allowed"),
                "Non-safe Constructor must also reject global tags in snakeyaml >= 2.0, got: " + exception.getMessage());
    }

    @Test
    void defaultYamlLoader_rejectsCve20221471GadgetPayload() {
        // given
        Yaml yaml = new Yaml();
        String document = "!!javax.script.ScriptEngineManager "
                + "[!!java.net.URLClassLoader [[!!java.net.URL [\"http://127.0.0.1:1/\"]]]]";

        // when
        YAMLException exception = assertThrows(YAMLException.class, () -> yaml.load(document));

        // then
        assertTrue(exception.getMessage().contains("Global tag is not allowed"),
                "CVE-2022-1471 gadget must be rejected, got: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("javax.script.ScriptEngineManager"),
                "Rejection must name the offending tag, got: " + exception.getMessage());
    }

    @Test
    void defaultYamlLoader_rejectsGlobalTagNestedInsideSafeMapping() {
        // given
        Yaml yaml = new Yaml();
        String document = """
                spring:
                  application:
                    name: educational-platform
                  payload: !!java.net.URI ["https://example.com"]
                """;

        // when
        YAMLException exception = assertThrows(YAMLException.class, () -> yaml.load(document));

        // then
        assertTrue(exception.getMessage().contains("Global tag is not allowed"),
                "Nested global tag must be rejected, got: " + exception.getMessage());
    }

    @Test
    void defaultYamlLoader_loadAll_rejectsGlobalTagInLaterDocument() {
        // given
        Yaml yaml = new Yaml();
        String documents = """
                server:
                  port: 8080
                ---
                payload: !!java.net.URI ["https://example.com"]
                """;
        Iterator<Object> iterator = yaml.loadAll(documents).iterator();

        // when
        Map<?, ?> first = assertInstanceOf(Map.class, iterator.next());
        YAMLException exception = assertThrows(YAMLException.class, iterator::next);

        // then
        assertEquals(Map.of("port", 8080), first.get("server"));
        assertTrue(exception.getMessage().contains("Global tag is not allowed"),
                "Global tag in a multi-document stream must be rejected, got: " + exception.getMessage());
    }
}
