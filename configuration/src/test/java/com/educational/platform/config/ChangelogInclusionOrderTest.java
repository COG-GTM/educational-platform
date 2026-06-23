package com.educational.platform.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the master Liquibase changelog includes module changelogs in the
 * correct order. The {@code common.yml} changelog creates the {@code failed_integration_events}
 * table, which must exist before any module handler can persist dead-letter records.
 * <p>
 * If common.yml is accidentally moved after a module changelog (or removed), all
 * integration event recovery will fail at runtime with a "table not found" error.
 */
class ChangelogInclusionOrderTest {

    private static final String MASTER_CHANGELOG_PATH = "db/changelog/db.changelog-master.yml";

    @Test
    void masterChangelog_commonIsIncludedFirst() throws Exception {
        List<String> includes = parseIncludedFiles();

        assertThat(includes).isNotEmpty();
        assertThat(includes.get(0))
                .as("common.yml must be the first include — it creates the failed_integration_events table")
                .contains("common.yml");
    }

    @Test
    void masterChangelog_includesAllExpectedModules() throws Exception {
        List<String> includes = parseIncludedFiles();

        assertThat(includes)
                .as("Master changelog should include all module changelogs")
                .anyMatch(f -> f.contains("common.yml"))
                .anyMatch(f -> f.contains("administration.yml"))
                .anyMatch(f -> f.contains("courses.yml"))
                .anyMatch(f -> f.contains("course-enrollments.yml"))
                .anyMatch(f -> f.contains("course-reviews.yml"))
                .anyMatch(f -> f.contains("users.yml"));
    }

    @Test
    void masterChangelog_commonComesBeforeAdministration() throws Exception {
        List<String> includes = parseIncludedFiles();

        int commonIdx = indexOf(includes, "common.yml");
        int adminIdx = indexOf(includes, "administration.yml");

        assertThat(commonIdx).as("common.yml index").isGreaterThanOrEqualTo(0);
        assertThat(adminIdx).as("administration.yml index").isGreaterThanOrEqualTo(0);
        assertThat(commonIdx).isLessThan(adminIdx);
    }

    @Test
    void masterChangelog_commonComesBeforeCourses() throws Exception {
        List<String> includes = parseIncludedFiles();

        int commonIdx = indexOf(includes, "common.yml");
        int coursesIdx = indexOf(includes, "courses.yml");

        assertThat(commonIdx).as("common.yml index").isGreaterThanOrEqualTo(0);
        assertThat(coursesIdx).as("courses.yml index").isGreaterThanOrEqualTo(0);
        assertThat(commonIdx).isLessThan(coursesIdx);
    }

    @Test
    void masterChangelog_hasNoDuplicateIncludes() throws Exception {
        List<String> includes = parseIncludedFiles();

        assertThat(includes)
                .as("No duplicate changelog includes")
                .doesNotHaveDuplicates();
    }

    private List<String> parseIncludedFiles() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(MASTER_CHANGELOG_PATH)) {
            assertThat(is).as("Master changelog file should exist on classpath").isNotNull();
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            List<String> files = new ArrayList<>();
            Pattern pattern = Pattern.compile("file:\\s*[\"']?([^\"'\\n]+)[\"']?");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                files.add(matcher.group(1).trim());
            }
            return files;
        }
    }

    private int indexOf(List<String> includes, String substring) {
        for (int i = 0; i < includes.size(); i++) {
            if (includes.get(i).contains(substring)) {
                return i;
            }
        }
        return -1;
    }
}
