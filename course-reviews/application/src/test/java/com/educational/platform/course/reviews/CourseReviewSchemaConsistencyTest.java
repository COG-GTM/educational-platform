package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.enrollment.ReviewerEnrollment;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the uniqueness contracts introduced for review creation on both schema sources: the JPA mapping (used by
 * Hibernate DDL in module tests) and the Liquibase changelog (used by the application). Both must agree on table,
 * columns and constraint name, otherwise the constraint name reported in {@code DataIntegrityViolationException}
 * and matched by {@code ReviewCourseCommandHandler} would differ depending on which one created the schema.
 */
public class CourseReviewSchemaConsistencyTest {

    private static final String CHANGELOG = "db/course-reviews.yml";

    private static final String COURSE_REVIEW_TABLE = "course_review";
    private static final String COURSE_REVIEW_CONSTRAINT = "course_review_reviewer_course_uk";
    private static final List<String> COURSE_REVIEW_COLUMNS = List.of("reviewer", "course");

    private static final String REVIEWER_ENROLLMENT_TABLE = "reviewer_enrollment";
    private static final String REVIEWER_ENROLLMENT_CONSTRAINT = "reviewer_enrollment_course_id_username_uk";
    private static final List<String> REVIEWER_ENROLLMENT_COLUMNS = List.of("course_id", "username");

    @Test
    void courseReviewEntity_reviewerCourseUniqueConstraintDeclared() {
        // given
        final Table table = CourseReview.class.getAnnotation(Table.class);

        // when
        final UniqueConstraint[] constraints = Objects.requireNonNull(table).uniqueConstraints();

        // then
        assertThat(table.name()).isEqualTo(COURSE_REVIEW_TABLE);
        assertThat(constraints).hasSize(1);
        assertThat(constraints[0].name()).isEqualTo(COURSE_REVIEW_CONSTRAINT);
        assertThat(constraints[0].columnNames()).containsExactlyElementsOf(COURSE_REVIEW_COLUMNS);
    }

    @Test
    void reviewerEnrollmentEntity_courseIdUsernameUniqueConstraintDeclared() {
        // given
        final Table table = ReviewerEnrollment.class.getAnnotation(Table.class);

        // when
        final UniqueConstraint[] constraints = Objects.requireNonNull(table).uniqueConstraints();

        // then
        assertThat(table.name()).isEqualTo(REVIEWER_ENROLLMENT_TABLE);
        assertThat(constraints).hasSize(1);
        assertThat(constraints[0].name()).isEqualTo(REVIEWER_ENROLLMENT_CONSTRAINT);
        assertThat(constraints[0].columnNames()).containsExactlyElementsOf(REVIEWER_ENROLLMENT_COLUMNS);
    }

    @Test
    void changelog_uniqueConstraintsMatchEntityMappings() throws IOException {
        // given
        final List<Map<String, Object>> changeSets = changeSets();

        // when
        final List<Map<String, Object>> uniqueConstraints = changes(changeSets, "addUniqueConstraint");

        // then
        assertThat(uniqueConstraints).hasSize(2);

        final Map<String, Object> courseReviewConstraint = uniqueConstraints.get(0);
        assertThat(courseReviewConstraint.get("tableName")).isEqualTo(COURSE_REVIEW_TABLE);
        assertThat(courseReviewConstraint.get("constraintName")).isEqualTo(COURSE_REVIEW_CONSTRAINT);
        assertThat(columnNames(courseReviewConstraint)).containsExactlyElementsOf(COURSE_REVIEW_COLUMNS);

        final Map<String, Object> reviewerEnrollmentConstraint = uniqueConstraints.get(1);
        assertThat(reviewerEnrollmentConstraint.get("tableName")).isEqualTo(REVIEWER_ENROLLMENT_TABLE);
        assertThat(reviewerEnrollmentConstraint.get("constraintName")).isEqualTo(REVIEWER_ENROLLMENT_CONSTRAINT);
        assertThat(columnNames(reviewerEnrollmentConstraint)).containsExactlyElementsOf(REVIEWER_ENROLLMENT_COLUMNS);
    }

    @Test
    void changelog_reviewerEnrollmentTableMatchesEntityMapping() throws IOException {
        // given
        final List<Map<String, Object>> changeSets = changeSets();

        // when
        final Map<String, Object> createTable = changes(changeSets, "createTable").stream()
                .filter(change -> REVIEWER_ENROLLMENT_TABLE.equals(change.get("tableName")))
                .findFirst()
                .orElseThrow();
        final List<Map<String, Object>> columns = list(createTable, "columns").stream()
                .map(column -> map(column, "column"))
                .toList();

        // then
        assertThat(columns).extracting(column -> column.get("name")).containsExactly("id", "course_id", "username");
        assertThat(columns).filteredOn(column -> REVIEWER_ENROLLMENT_COLUMNS.contains((String) column.get("name")))
                .allSatisfy(column -> assertThat(map(column, "constraints").get("nullable")).isEqualTo(false));
    }

    @Test
    void changelog_uniqueConstraintsAppliedAfterTableCreationWithUniqueChangeSetIds() throws IOException {
        // given
        final List<Map<String, Object>> changeSets = changeSets();

        // when
        final int courseReviewTableIndex = indexOfChange(changeSets, "createTable", COURSE_REVIEW_TABLE);
        final int courseReviewConstraintIndex = indexOfChange(changeSets, "addUniqueConstraint", COURSE_REVIEW_TABLE);
        final int reviewerEnrollmentTableIndex = indexOfChange(changeSets, "createTable", REVIEWER_ENROLLMENT_TABLE);
        final int reviewerEnrollmentConstraintIndex = indexOfChange(changeSets, "addUniqueConstraint", REVIEWER_ENROLLMENT_TABLE);

        // then
        assertThat(courseReviewTableIndex).isNotNegative();
        assertThat(courseReviewConstraintIndex).isGreaterThan(courseReviewTableIndex);
        assertThat(reviewerEnrollmentTableIndex).isNotNegative();
        assertThat(reviewerEnrollmentConstraintIndex).isGreaterThanOrEqualTo(reviewerEnrollmentTableIndex);
        assertThat(changeSets.stream().map(changeSet -> changeSet.get("id"))).doesNotHaveDuplicates();
    }

    private static int indexOfChange(List<Map<String, Object>> changeSets, String changeType, String tableName) {
        for (int i = 0; i < changeSets.size(); i++) {
            final boolean matches = list(changeSets.get(i), "changes").stream()
                    .filter(change -> change.containsKey(changeType))
                    .map(change -> map(change, changeType))
                    .anyMatch(change -> tableName.equals(change.get("tableName")));
            if (matches) {
                return i;
            }
        }
        return -1;
    }

    private static List<Map<String, Object>> changes(List<Map<String, Object>> changeSets, String changeType) {
        return changeSets.stream()
                .map(changeSet -> list(changeSet, "changes"))
                .flatMap(List::stream)
                .filter(change -> change.containsKey(changeType))
                .map(change -> map(change, changeType))
                .toList();
    }

    private static List<String> columnNames(Map<String, Object> constraint) {
        return Arrays.stream(((String) constraint.get("columnNames")).split(","))
                .map(String::trim)
                .toList();
    }

    private static List<Map<String, Object>> changeSets() throws IOException {
        try (InputStream changelog = CourseReviewSchemaConsistencyTest.class.getClassLoader().getResourceAsStream(CHANGELOG)) {
            final Map<String, Object> root = new Yaml().load(Objects.requireNonNull(changelog, CHANGELOG + " not on classpath"));
            return list(root, "databaseChangeLog").stream()
                    .map(entry -> map(entry, "changeSet"))
                    .toList();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> parent, String key) {
        return (List<Map<String, Object>>) parent.get(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map<String, Object> parent, String key) {
        return (Map<String, Object>) parent.get(key);
    }
}
