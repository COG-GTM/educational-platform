package com.educational.platform.jackson;

import com.educational.platform.courses.CreateCourseRequest;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.NumberOfStudents;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
    void nestingDepthAtExactDefaultLimit_parsesSuccessfully() throws Exception {
        var depth = 1000;
        var json = buildDeeplyNestedJson(depth);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node).isNotNull();
    }

    @Test
    void deeplyNestedArrays_exceedingLimit_throwsException() {
        var depth = 1500;
        var json = buildDeeplyNestedArrayJson(depth);

        assertThatThrownBy(() -> objectMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void moderatelyNestedArrays_withinLimit_parsesSuccessfully() throws Exception {
        var depth = 100;
        var json = buildDeeplyNestedArrayJson(depth);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node).isNotNull();
    }

    @Test
    void numberLengthAtExactDefaultLimit_parsesSuccessfully() throws Exception {
        var number = "1" + "0".repeat(999);
        var json = "{\"value\": " + number + "}";

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("value")).isNotNull();
    }

    @Test
    void numberLengthJustOverDefaultLimit_throwsException() {
        var number = "1" + "0".repeat(1000);
        var json = "{\"value\": " + number + "}";

        assertThatThrownBy(() -> objectMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void defaultConstraintValues_matchExpectedDefaults() {
        var constraints = objectMapper.getFactory().streamReadConstraints();

        assertThat(constraints.getMaxNestingDepth()).isEqualTo(1000);
        assertThat(constraints.getMaxNumberLength()).isEqualTo(1000);
        assertThat(constraints.getMaxStringLength()).isEqualTo(20_000_000);
    }

    @Test
    void combinedCustomConstraints_allEnforcedSimultaneously() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(10)
                        .maxNumberLength(50)
                        .maxStringLength(100)
                        .build()
        );

        var deepJson = buildDeeplyNestedJson(20);
        assertThatThrownBy(() -> restrictedMapper.readTree(deepJson))
                .isInstanceOf(StreamConstraintsException.class);

        var longNumberJson = "{\"v\": " + "1".repeat(60) + "}";
        assertThatThrownBy(() -> restrictedMapper.readTree(longNumberJson))
                .isInstanceOf(StreamConstraintsException.class);

        var longStringJson = "{\"v\": \"" + "x".repeat(200) + "\"}";
        assertThatThrownBy(() -> restrictedMapper.readTree(longStringJson))
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

    @Test
    void streamWriteConstraints_defaultMaxNestingDepth_isConfigured() {
        var constraints = objectMapper.getFactory().streamWriteConstraints();

        assertThat(constraints.getMaxNestingDepth()).isGreaterThan(0);
    }

    @Test
    void streamWriteConstraints_customMaxNestingDepth_enforced() throws Exception {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamWriteConstraints(
                StreamWriteConstraints.builder()
                        .maxNestingDepth(3)
                        .build()
        );

        Map<String, Object> nested = Map.of("leaf", "value");
        for (int i = 0; i < 10; i++) {
            nested = Map.of("level" + i, nested);
        }
        final Map<String, Object> deeplyNested = nested;

        assertThatThrownBy(() -> restrictedMapper.writeValueAsString(deeplyNested))
                .hasRootCauseInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void constraintsEnforced_viaReadValue_notJustReadTree() {
        var depth = 1500;
        var json = buildDeeplyNestedJson(depth);

        assertThatThrownBy(() -> objectMapper.readValue(json, JsonNode.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void constraintsEnforced_onInputStreamParsing() {
        var depth = 1500;
        var json = buildDeeplyNestedJson(depth);
        var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> objectMapper.readTree(inputStream))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void inputStreamParsing_withinLimits_parsesSuccessfully() throws Exception {
        var json = "{\"key\": \"value\"}";
        var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonNode node = objectMapper.readTree(inputStream);
        assertThat(node.get("key").asText()).isEqualTo("value");
    }

    @Test
    void mixedNestedObjectsAndArrays_exceedingLimit_throwsException() {
        var json = buildMixedNestedJson(1500);

        assertThatThrownBy(() -> objectMapper.readTree(json))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void mixedNestedObjectsAndArrays_withinLimit_parsesSuccessfully() throws Exception {
        var json = buildMixedNestedJson(50);

        JsonNode node = objectMapper.readTree(json);
        assertThat(node).isNotNull();
    }

    @Test
    void negativeNumber_withinLimit_parsesSuccessfully() throws Exception {
        var json = "{\"value\": -12345}";

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("value").asInt()).isEqualTo(-12345);
    }

    @Test
    void floatingPointNumber_withinLimit_parsesSuccessfully() throws Exception {
        var json = "{\"value\": 3.141592653589793}";

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("value").asDouble()).isEqualTo(3.141592653589793);
    }

    @Test
    void constraintsArePerFactory_notSharedAcrossMappers() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(5)
                        .build()
        );

        var defaultConstraints = objectMapper.getFactory().streamReadConstraints();
        var restrictedConstraints = restrictedMapper.getFactory().streamReadConstraints();

        assertThat(defaultConstraints.getMaxNestingDepth()).isEqualTo(1000);
        assertThat(restrictedConstraints.getMaxNestingDepth()).isEqualTo(5);
    }

    private String buildMixedNestedJson(int depth) {
        var sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i % 2 == 0) {
                sb.append("{\"a\":");
            } else {
                sb.append("[");
            }
        }
        sb.append("1");
        for (int i = depth - 1; i >= 0; i--) {
            if (i % 2 == 0) {
                sb.append("}");
            } else {
                sb.append("]");
            }
        }
        return sb.toString();
    }

    @Test
    void maxDocumentLength_canBeConfiguredExplicitly() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxDocumentLength(5000)
                        .build()
        );

        var constraints = restrictedMapper.getFactory().streamReadConstraints();

        assertThat(constraints.hasMaxDocumentLength()).isTrue();
        assertThat(constraints.getMaxDocumentLength()).isEqualTo(5000);
    }

    @Test
    void maxDocumentLength_customLimit_exceedingThreshold_throwsException() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxDocumentLength(100)
                        .build()
        );

        var longJson = "{\"value\": \"" + "x".repeat(200) + "\"}";

        assertThatThrownBy(() -> restrictedMapper.readTree(longJson))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void maxDocumentLength_withinCustomLimit_parsesSuccessfully() throws Exception {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxDocumentLength(1000)
                        .build()
        );

        var json = "{\"value\": \"" + "x".repeat(50) + "\"}";

        var node = restrictedMapper.readTree(json);
        assertThat(node.get("value").asText()).hasSize(50);
    }

    @Test
    void allConstraints_combinedWithDocumentLength_enforced() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(10)
                        .maxNumberLength(50)
                        .maxStringLength(100)
                        .maxDocumentLength(500)
                        .build()
        );

        var longDocJson = "{\"value\": \"" + "y".repeat(600) + "\"}";
        assertThatThrownBy(() -> restrictedMapper.readTree(longDocJson))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void streamReadConstraints_builder_preservesAllCustomValues() {
        var constraints = StreamReadConstraints.builder()
                .maxNestingDepth(42)
                .maxNumberLength(99)
                .maxStringLength(5000)
                .maxDocumentLength(10000)
                .build();

        assertThat(constraints.getMaxNestingDepth()).isEqualTo(42);
        assertThat(constraints.getMaxNumberLength()).isEqualTo(99);
        assertThat(constraints.getMaxStringLength()).isEqualTo(5000);
        assertThat(constraints.getMaxDocumentLength()).isEqualTo(10000);
    }

    private String buildDeeplyNestedArrayJson(int depth) {
        var sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("[");
        }
        sb.append("1");
        for (int i = 0; i < depth; i++) {
            sb.append("]");
        }
        return sb.toString();
    }

    // --- StreamWriteConstraints default values ---

    @Test
    void streamWriteConstraints_defaultValues_matchExpectedDefaults() {
        var constraints = objectMapper.getFactory().streamWriteConstraints();

        assertThat(constraints.getMaxNestingDepth()).isEqualTo(1000);
    }

    // --- Constraint enforcement on actual project DTOs via readValue ---

    @Test
    void customStringConstraint_enforcedOnDtoReadValue() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(10)
                        .build()
        );

        var longName = "x".repeat(500);
        var json = "{\"name\":\"" + longName + "\",\"description\":\"short\"}";

        assertThatThrownBy(() -> restrictedMapper.readValue(json, CreateCourseRequest.class))
                .hasRootCauseInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void defaultStringConstraint_allowsNormalDtoReadValue() throws Exception {
        var name = "Normal Course Name";
        var json = "{\"name\":\"" + name + "\",\"description\":\"A normal description\"}";

        var result = objectMapper.readValue(json, CreateCourseRequest.class);

        assertThat(result.name()).isEqualTo(name);
    }

    @Test
    void customNumberConstraint_enforcedOnPrimitiveRecordField() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNumberLength(5)
                        .build()
        );

        var json = "{\"number\": 1234567890}";

        assertThatThrownBy(() -> restrictedMapper.readValue(json, NumberOfStudents.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void defaultNumberConstraint_allowsNormalDtoValue() throws Exception {
        var json = "{\"rating\": 4.75}";

        var result = objectMapper.readValue(json, CourseRating.class);

        assertThat(result.rating()).isEqualTo(4.75);
    }

    // --- Combined read+write constraints on same mapper ---

    @Test
    void combinedReadAndWriteConstraints_bothEnforced() throws Exception {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(5)
                        .build()
        );
        restrictedMapper.getFactory().setStreamWriteConstraints(
                StreamWriteConstraints.builder()
                        .maxNestingDepth(3)
                        .build()
        );

        var readConstraints = restrictedMapper.getFactory().streamReadConstraints();
        var writeConstraints = restrictedMapper.getFactory().streamWriteConstraints();

        assertThat(readConstraints.getMaxNestingDepth()).isEqualTo(5);
        assertThat(writeConstraints.getMaxNestingDepth()).isEqualTo(3);

        var deepJson = buildDeeplyNestedJson(10);
        assertThatThrownBy(() -> restrictedMapper.readTree(deepJson))
                .isInstanceOf(StreamConstraintsException.class);
    }
}
