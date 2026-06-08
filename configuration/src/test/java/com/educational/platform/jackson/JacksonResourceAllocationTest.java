package com.educational.platform.jackson;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the upgraded Jackson Core enforces resource allocation constraints.
 * These tests validate the fix for CVEs related to "Allocation of Resources Without
 * Limits or Throttling" (SNYK-JAVA-COMFABORXMLJACKSONCORE-* and SNYK-JAVA-TOOLSJACKSONCORE-*).
 *
 * Jackson 2.16+ introduced StreamReadConstraints with default limits:
 * - Max nesting depth: 1000
 * - Max number length: 1000
 * - Max string length: 20_000_000
 */
class JacksonResourceAllocationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void defaultConstraints_areConfigured() {
        var factory = objectMapper.getFactory();
        var constraints = factory.streamReadConstraints();

        assertThat(constraints.getMaxNestingDepth()).isGreaterThan(0);
        assertThat(constraints.getMaxNumberLength()).isGreaterThan(0);
        assertThat(constraints.getMaxStringLength()).isGreaterThan(0);
    }

    @Test
    void deeplyNestedJson_exceedingDefaultLimit_throwsException() {
        var depth = 1500;
        var json = buildDeeplyNestedJson(depth);

        assertThatThrownBy(() -> objectMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class)
                .hasMessageContaining("nesting");
    }

    @Test
    void moderatelyNestedJson_withinLimit_parsesSuccessfully() throws Exception {
        var depth = 100;
        var json = buildDeeplyNestedJson(depth);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node).isNotNull();
    }

    @Test
    void customConstraints_lowerNestingDepth_enforced() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(50)
                        .build()
        );

        var json = buildDeeplyNestedJson(100);

        assertThatThrownBy(() -> restrictedMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class)
                .hasMessageContaining("nesting");
    }

    @Test
    void validJsonWithinAllLimits_parsesSuccessfully() throws Exception {
        var json = """
                {
                    "name": "Test Course",
                    "description": "A valid course description",
                    "nested": {
                        "level1": {
                            "level2": {
                                "value": 42
                            }
                        }
                    }
                }
                """;

        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("name").asText()).isEqualTo("Test Course");
        assertThat(node.at("/nested/level1/level2/value").asInt()).isEqualTo(42);
    }

    @Test
    void extremelyLongNumber_exceedingLimit_throwsException() {
        var longNumber = "1" + "0".repeat(1500);
        var json = "{\"value\": " + longNumber + "}";

        assertThatThrownBy(() -> objectMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void numberWithinLimit_parsesSuccessfully() throws Exception {
        var normalNumber = "1" + "0".repeat(100);
        var json = "{\"value\": " + normalNumber + "}";

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("value")).isNotNull();
    }

    @Test
    void nestingDepthJustOverDefaultLimit_throwsException() {
        var depth = 1001;
        var json = buildDeeplyNestedJson(depth);

        assertThatThrownBy(() -> objectMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class)
                .hasMessageContaining("nesting");
    }

    @Test
    void customStringLengthConstraint_exceedingLimit_throwsException() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(100)
                        .build()
        );

        var longValue = "x".repeat(200);
        var json = "{\"value\": \"" + longValue + "\"}";

        assertThatThrownBy(() -> restrictedMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void stringWithinCustomLimit_parsesSuccessfully() throws Exception {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(500)
                        .build()
        );

        var value = "x".repeat(100);
        var json = "{\"value\": \"" + value + "\"}";

        JsonNode node = restrictedMapper.readTree(json);
        assertThat(node.get("value").asText()).hasSize(100);
    }

    @Test
    void customNumberLengthConstraint_exceedingLimit_throwsException() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNumberLength(50)
                        .build()
        );

        var longNumber = "1" + "0".repeat(100);
        var json = "{\"value\": " + longNumber + "}";

        assertThatThrownBy(() -> restrictedMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void emptyJsonObject_parsesSuccessfully() throws Exception {
        JsonNode node = objectMapper.readTree("{}");
        assertThat(node.isEmpty()).isTrue();
    }

    @Test
    void emptyJsonArray_parsesSuccessfully() throws Exception {
        JsonNode node = objectMapper.readTree("[]");
        assertThat(node.isEmpty()).isTrue();
    }

    @Test
    void malformedJson_throwsException() {
        assertThatThrownBy(() -> objectMapper.readTree("{invalid json}"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void nullInput_throwsException() {
        assertThatThrownBy(() -> objectMapper.readTree((String) null))
                .isInstanceOf(Exception.class);
    }

    private String buildDeeplyNestedJson(int depth) {
        var sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{\"a\":");
        }
        sb.append("1");
        for (int i = 0; i < depth; i++) {
            sb.append("}");
        }
        return sb.toString();
    }
}
