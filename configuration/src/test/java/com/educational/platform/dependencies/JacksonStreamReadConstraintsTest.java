package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CWE-770 (SNYK-JAVA-COMFASTERXMLJACKSONCORE-6248036): the resolved jackson-core must bound the resources
 * a single untrusted document can consume by default.
 */
class JacksonStreamReadConstraintsTest {

    private final ObjectMapper sut = new ObjectMapper();

    @Test
    void objectMapper_usesBoundedDefaultReadConstraints() {
        // given
        StreamReadConstraints constraints = sut.getFactory().streamReadConstraints();

        // when / then
        assertEquals(1000, constraints.getMaxNestingDepth());
        assertEquals(1000, constraints.getMaxNumberLength());
        assertTrue(constraints.getMaxStringLength() > 0);
    }

    @Test
    void readTree_nestingAtLimit_isParsed() throws Exception {
        // given
        String json = nestedArrays(StreamReadConstraints.DEFAULT_MAX_DEPTH);

        // when
        JsonNode node = sut.readTree(json);

        // then
        assertTrue(node.isArray());
    }

    @Test
    void readTree_nestingAboveLimit_isRejected() {
        // given
        String json = nestedArrays(StreamReadConstraints.DEFAULT_MAX_DEPTH + 1);

        // when
        StreamConstraintsException exception = assertThrows(StreamConstraintsException.class, () -> sut.readTree(json));

        // then
        assertTrue(exception.getMessage().contains("nesting depth"), exception::getMessage);
    }

    @Test
    void readTree_numberLongerThanLimit_isRejected() {
        // given
        String json = "{\"value\":" + "9".repeat(StreamReadConstraints.DEFAULT_MAX_NUM_LEN + 1) + "}";

        // when
        StreamConstraintsException exception = assertThrows(StreamConstraintsException.class, () -> sut.readTree(json));

        // then
        assertTrue(exception.getMessage().contains("Number value length"), exception::getMessage);
    }

    private static String nestedArrays(int depth) {
        return "[".repeat(depth) + "]".repeat(depth);
    }
}
