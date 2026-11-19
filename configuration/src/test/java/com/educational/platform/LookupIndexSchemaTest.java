package com.educational.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:lookup-index-schema-test",
        "spring.jpa.hibernate.ddl-auto=update"
})
class LookupIndexSchemaTest {

    @Autowired
    private DataSource dataSource;

    @ParameterizedTest
    @CsvSource({
            "CUSTOM_USER, CUSTOM_USER_USERNAME_IDX, USERNAME, 'lookup-user'",
            "STUDENT, STUDENT_USERNAME_IDX, USERNAME, 'lookup-user'",
            "REVIEWER, REVIEWER_USERNAME_IDX, USERNAME, 'lookup-user'",
            "TEACHER, TEACHER_USERNAME_IDX, USERNAME, 'lookup-user'",
            "COURSE_ENROLLMENT, COURSE_ENROLLMENT_STUDENT_IDX, STUDENT, 1"
    })
    void equalityLookup_usesLookupIndex(String table, String index, String column, String value)
            throws SQLException {
        // given
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "EXPLAIN SELECT * FROM " + table + " WHERE " + column + " = ?")) {
            statement.setString(1, value);

            // when
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                var plan = result.getString(1);

                // then
                assertTrue(plan.contains(index), plan);
            }
        }
    }

    @Test
    void studentUsername_allowsDuplicates() throws SQLException {
        // given
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var insert = connection.prepareStatement("INSERT INTO student (username) VALUES (?)");
                 var count = connection.prepareStatement("SELECT COUNT(*) FROM student WHERE username = ?")) {
                insert.setString(1, "duplicate-lookup-user");
                count.setString(1, "duplicate-lookup-user");

                // when
                insert.executeUpdate();
                insert.executeUpdate();

                // then
                try (var result = count.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(2, result.getInt(1));
                }
            } finally {
                connection.rollback();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
            "CUSTOM_USER, CUSTOM_USER_USERNAME_IDX, USERNAME",
            "STUDENT, STUDENT_USERNAME_IDX, USERNAME",
            "REVIEWER, REVIEWER_USERNAME_IDX, USERNAME",
            "TEACHER, TEACHER_USERNAME_IDX, USERNAME",
            "COURSE_ENROLLMENT, COURSE_ENROLLMENT_STUDENT_IDX, STUDENT"
    })
    void schemaUpdate_createsNonUniqueSingleColumnLookupIndex(String table, String index, String column)
            throws SQLException {
        var indexedColumns = new ArrayList<String>();

        try (var connection = dataSource.getConnection();
             var indexes = connection.getMetaData().getIndexInfo(
                     connection.getCatalog(), connection.getSchema(), table, false, false)) {
            while (indexes.next()) {
                if (index.equals(indexes.getString("INDEX_NAME"))) {
                    assertTrue(indexes.getBoolean("NON_UNIQUE"), index + " must not enforce uniqueness");
                    indexedColumns.add(indexes.getString("COLUMN_NAME"));
                }
            }
        }

        assertEquals(List.of(column), indexedColumns, "Columns for " + index);
    }
}
