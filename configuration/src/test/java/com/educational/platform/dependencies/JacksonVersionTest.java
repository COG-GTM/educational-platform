package com.educational.platform.dependencies;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-bom.version} override: jackson-databind below 3.1.5 is affected by
 * CVE-2026-59889 (GHSA-5gvw-p9qm-jgwh).
 */
public class JacksonVersionTest {

    @Test
    void jacksonDatabind_isAtLeast_3_1_5() {
        final Version resolved = tools.jackson.databind.cfg.PackageVersion.VERSION;

        assertTrue(resolved.compareTo(new Version(3, 1, 5, null, resolved.getGroupId(), resolved.getArtifactId())) >= 0,
                "tools.jackson.core:jackson-databind must be >= 3.1.5 (CVE-2026-59889), but was " + resolved);
    }

    @Test
    void jacksonCore_matchesDatabindLine() {
        final Version core = tools.jackson.core.json.PackageVersion.VERSION;

        assertTrue(core.compareTo(new Version(3, 1, 5, null, core.getGroupId(), core.getArtifactId())) >= 0,
                "tools.jackson.core:jackson-core must be >= 3.1.5, but was " + core);
    }

    @Test
    void jacksonAnnotations_providesTypesRequiredByDatabind() {
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"),
                "com.fasterxml.jackson.core:jackson-annotations must be >= 2.21 for jackson-databind 3.1.x");
    }

    @Test
    void jacksonCore_andDatabind_shareMajorMinor() {
        final Version core = tools.jackson.core.json.PackageVersion.VERSION;
        final Version databind = tools.jackson.databind.cfg.PackageVersion.VERSION;

        assertEquals(databind.getMajorVersion(), core.getMajorVersion());
        assertEquals(databind.getMinorVersion(), core.getMinorVersion());
    }

    @Test
    void jsonMapper_roundTripsAnnotatedType_withResolvedAnnotationsAndDatabind() throws Exception {
        final JsonMapper mapper = JsonMapper.builder().build();
        final CourseView course = new CourseView("DDD", 5, null);

        assertEquals("{\"course_name\":\"DDD\",\"rating\":5}", mapper.writeValueAsString(course));
        assertEquals(course, mapper.readValue("{\"course_name\":\"DDD\",\"rating\":5}", CourseView.class));
    }

    private static record CourseView(
            @JsonProperty("course_name") String name,
            int rating,
            @JsonInclude(JsonInclude.Include.NON_NULL) String description
    ) {
    }
}
