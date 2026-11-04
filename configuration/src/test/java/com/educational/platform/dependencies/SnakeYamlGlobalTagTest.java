package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2022-1471: snakeyaml < 2.0 instantiates arbitrary classes from global tags.
 */
class SnakeYamlGlobalTagTest {

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
}
