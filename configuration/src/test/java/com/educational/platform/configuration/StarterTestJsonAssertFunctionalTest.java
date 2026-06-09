package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Functionally validates the JSONassert library provided transitively by
 * {@code spring-boot-starter-test}. Existing tests
 * ({@link StarterTestTransitiveDependencyPresenceTest}) verify classpath
 * presence; this test exercises actual JSON comparison to prove the
 * library is fully functional for API response validation.
 * <p>
 * JSONassert is critical for testing REST API responses in the educational
 * platform's web modules. It provides flexible JSON comparison modes
 * (STRICT, LENIENT, NON_EXTENSIBLE) that are preferable to string-based
 * comparison because they handle field ordering and whitespace differences.
 */
class StarterTestJsonAssertFunctionalTest {

    @Test
    void strictMode_shouldPassForIdenticalJson() {
        assertThatCode(() -> JSONAssert.assertEquals(
                "{\"name\":\"course\",\"id\":1}",
                "{\"name\":\"course\",\"id\":1}",
                JSONCompareMode.STRICT))
                .as("STRICT mode must pass when JSON objects are structurally identical")
                .doesNotThrowAnyException();
    }

    @Test
    void lenientMode_shouldPassWhenExpectedIsSubset() {
        assertThatCode(() -> JSONAssert.assertEquals(
                "{\"name\":\"course\"}",
                "{\"name\":\"course\",\"id\":1,\"status\":\"active\"}",
                JSONCompareMode.LENIENT))
                .as("LENIENT mode must pass when expected JSON is a subset of actual — "
                        + "this is the recommended mode for API tests where extra fields are acceptable")
                .doesNotThrowAnyException();
    }

    @Test
    void strictMode_shouldFailWhenExtraFieldPresent() {
        assertThatThrownBy(() -> JSONAssert.assertEquals(
                "{\"name\":\"course\"}",
                "{\"name\":\"course\",\"id\":1}",
                JSONCompareMode.STRICT))
                .as("STRICT mode must fail when actual JSON has fields not in expected")
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void jsonAssert_shouldHandleNestedObjects() {
        assertThatCode(() -> JSONAssert.assertEquals(
                "{\"course\":{\"title\":\"Java 101\"}}",
                "{\"course\":{\"title\":\"Java 101\"}}",
                JSONCompareMode.STRICT))
                .as("JSONAssert must support nested object comparison")
                .doesNotThrowAnyException();
    }

    @Test
    void jsonAssert_shouldHandleArrays() {
        assertThatCode(() -> JSONAssert.assertEquals(
                "[{\"id\":1},{\"id\":2}]",
                "[{\"id\":1},{\"id\":2}]",
                JSONCompareMode.STRICT))
                .as("JSONAssert must support JSON array comparison")
                .doesNotThrowAnyException();
    }

    @Test
    void nonExtensibleMode_shouldRejectExtraFields() {
        assertThatThrownBy(() -> JSONAssert.assertEquals(
                "{\"name\":\"course\"}",
                "{\"name\":\"course\",\"extra\":true}",
                JSONCompareMode.NON_EXTENSIBLE))
                .as("NON_EXTENSIBLE mode must reject extra fields while allowing different ordering — "
                        + "useful for testing contract compliance in modular monolith APIs")
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void jsonCompareMode_shouldHaveExpectedValues() {
        assertThat(JSONCompareMode.values())
                .as("JSONCompareMode must contain all expected comparison strategies")
                .extracting(Enum::name)
                .contains("STRICT", "LENIENT", "NON_EXTENSIBLE", "STRICT_ORDER");
    }
}
