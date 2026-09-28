package com.educational.platform.users;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the username uniqueness contract on both schema sources: the JPA mapping (used by Hibernate DDL) and the
 * Liquibase changelog. Both must agree on table, column and constraint name, otherwise the constraint name reported
 * in {@code DataIntegrityViolationException} would differ depending on which one created the schema.
 */
public class UserSchemaConsistencyTest {

    private static final String CHANGELOG = "db/users.yml";
    private static final String TABLE = "custom_user";
    private static final String COLUMN = "username";
    private static final String CONSTRAINT = "custom_user_username_uk";

    @Test
    void userEntity_usernameUniqueConstraintDeclared() {
        // given
        final Table table = User.class.getAnnotation(Table.class);

        // when
        final UniqueConstraint[] constraints = Objects.requireNonNull(table).uniqueConstraints();

        // then
        assertThat(table.name()).isEqualTo(TABLE);
        assertThat(constraints).hasSize(1);
        assertThat(constraints[0].name()).isEqualTo(CONSTRAINT);
        assertThat(constraints[0].columnNames()).containsExactly(COLUMN);
    }

    @Test
    void changelog_usernameUniqueConstraintMatchesEntityMapping() throws IOException {
        // given
        final List<Map<String, Object>> changeSets = changeSets();

        // when
        final List<Map<String, Object>> uniqueConstraints = changeSets.stream()
                .map(changeSet -> list(changeSet, "changes"))
                .flatMap(List::stream)
                .filter(change -> change.containsKey("addUniqueConstraint"))
                .map(change -> map(change, "addUniqueConstraint"))
                .toList();

        // then
        assertThat(uniqueConstraints).hasSize(1);
        final Map<String, Object> constraint = uniqueConstraints.getFirst();
        assertThat(constraint.get("tableName")).isEqualTo(TABLE);
        assertThat(constraint.get("columnNames")).isEqualTo(COLUMN);
        assertThat(constraint.get("constraintName")).isEqualTo(CONSTRAINT);
    }

    @Test
    void changelog_uniqueConstraintChangeSetAppliedAfterTableCreation() throws IOException {
        // given
        final List<Map<String, Object>> changeSets = changeSets();

        // when
        final int createTableIndex = indexOfChange(changeSets, "createTable");
        final int uniqueConstraintIndex = indexOfChange(changeSets, "addUniqueConstraint");

        // then
        assertThat(createTableIndex).isNotNegative();
        assertThat(uniqueConstraintIndex).isGreaterThan(createTableIndex);
        assertThat(changeSets.stream().map(changeSet -> changeSet.get("id"))).doesNotHaveDuplicates();
    }

    private static int indexOfChange(List<Map<String, Object>> changeSets, String changeType) {
        for (int i = 0; i < changeSets.size(); i++) {
            if (list(changeSets.get(i), "changes").stream().anyMatch(change -> change.containsKey(changeType))) {
                return i;
            }
        }
        return -1;
    }

    private static List<Map<String, Object>> changeSets() throws IOException {
        try (InputStream changelog = UserSchemaConsistencyTest.class.getClassLoader().getResourceAsStream(CHANGELOG)) {
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
